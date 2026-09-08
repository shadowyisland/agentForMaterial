package com.ruoyi.system.service;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * 在原 DOCX 中填充标记，字体、段落、编号、表格和其他包内容由模板控制。
 * {{字段.子字段}} 替换文字；独占段落的 {{+字段}} 按换行或数组复制段落；
 * 行内 {{#数组字段}} 按数组复制样板行，{{.子字段}} 引用当前项，{{@序号}} 从 1 计数。
 */
public class DocumentWordTemplateRenderer
{
    private static final String W = "http://schemas.openxmlformats.org/wordprocessingml/2006/main";
    private static final Pattern TOKEN = Pattern.compile("\\{\\{([^{}]+)}}");
    private static final Pattern ROW = Pattern.compile("\\{\\{#([^{}]+)}}");
    private static final Pattern PARAGRAPH = Pattern.compile("\\{\\{\\+([^{}]+)}}");

    public void render(InputStream template, Map<String, Object> values, OutputStream output) throws Exception
    {
        // 先在内存中完成文件，失败时不向下载响应写入半份 Word。
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try (ZipInputStream zip = new ZipInputStream(template);
                ZipOutputStream result = new ZipOutputStream(buffer))
        {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null)
            {
                byte[] bytes = readBytes(zip);
                String name = entry.getName();
                if (name.equals("word/document.xml") || name.matches("word/(header|footer)[^/]*\\.xml"))
                {
                    bytes = renderXml(bytes, values);
                }
                ZipEntry target = new ZipEntry(name);
                target.setTime(entry.getTime());
                result.putNextEntry(target);
                result.write(bytes);
                result.closeEntry();
            }
        }
        buffer.writeTo(output);
    }

    private byte[] renderXml(byte[] xml, Map<String, Object> values) throws Exception
    {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        Document document = factory.newDocumentBuilder().parse(new ByteArrayInputStream(xml));
        renderChildren(document.getDocumentElement(), values, values, 0);
        TransformerFactory transformers = TransformerFactory.newInstance();
        transformers.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
        Transformer transformer = transformers.newTransformer();
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        transformer.transform(new DOMSource(document), new StreamResult(output));
        return output.toByteArray();
    }

    private void renderChildren(Node parent, Map<String, Object> root, Object item, int index)
    {
        for (Node node : children(parent))
        {
            if (is(node, "tr"))
            {
                // 只检查当前行，嵌套表格由递归处理，避免外层整张表被重复。
                Matcher marker = ROW.matcher(rowText(node));
                if (marker.find())
                {
                    List<?> items = items(resolve(root, item, marker.group(1), index), false);
                    for (int i = 0; i < items.size(); i++)
                    {
                        // 模板默认数组项可能只包含“类别/单位”等说明文字；没有实际数据时不导出该行。
                        if (shouldRemoveRow(node, root, items.get(i), i + 1))
                        {
                            continue;
                        }
                        Node copy = node.cloneNode(true);
                        if (i > 0)
                        {
                            continueMergedCells(copy);
                        }
                        parent.insertBefore(copy, node);
                        renderChildren(copy, root, items.get(i), i + 1);
                    }
                    parent.removeChild(node);
                    continue;
                }
                // 用户在解析表单删除了静态规格行时，对应字段不再存在。
                // 模板中该行的所有字段标记都无法解析，整行一并移除。
                if (shouldRemoveRow(node, root, item, index))
                {
                    parent.removeChild(node);
                    continue;
                }
            }
            if (is(node, "p"))
            {
                Matcher marker = PARAGRAPH.matcher(paragraphText(node).trim());
                if (marker.matches())
                {
                    // 空的多行字段直接移除占位段落，避免 Word 中留下空白内容。
                    List<?> lines = items(resolve(root, item, marker.group(1), index), false);
                    for (Object line : lines)
                    {
                        Node copy = node.cloneNode(true);
                        parent.insertBefore(copy, node);
                        replaceTokens(copy, root, item, index, display(line));
                    }
                    parent.removeChild(node);
                }
                else
                {
                    replaceTokens(node, root, item, index, null);
                    // 文本框中的段落也可能带有标记。
                    renderChildren(node, root, item, index);
                }
                continue;
            }
            renderChildren(node, root, item, index);
        }
    }

    private void continueMergedCells(Node row)
    {
        for (Node cell : children(row))
        {
            if (!is(cell, "tc"))
            {
                continue;
            }
            for (Node properties : children(cell))
            {
                if (is(properties, "tcPr"))
                {
                    for (Node merge : children(properties))
                    {
                        if (is(merge, "vMerge") && "restart".equals(((Element) merge).getAttributeNS(W, "val")))
                        {
                            ((Element) merge).setAttributeNS(W, "w:val", "continue");
                            for (Node paragraph : children(cell))
                            {
                                if (is(paragraph, "p"))
                                {
                                    for (Element text : textNodes(paragraph))
                                    {
                                        text.setTextContent("");
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private void replaceTokens(Node paragraph, Map<String, Object> root, Object item, int index, String line)
    {
        List<Element> texts = textNodes(paragraph);
        StringBuilder text = new StringBuilder();
        List<Integer> offsets = new ArrayList<Integer>();
        for (Element element : texts)
        {
            offsets.add(text.length());
            text.append(element.getTextContent());
        }
        Matcher matcher = TOKEN.matcher(text);
        List<int[]> ranges = new ArrayList<int[]>();
        List<String> replacements = new ArrayList<String>();
        while (matcher.find())
        {
            String key = matcher.group(1);
            ranges.add(new int[] { matcher.start(), matcher.end() });
            replacements.add(key.startsWith("#") ? "" : key.startsWith("+") && line != null ? line
                    : display(resolve(root, item, key, index)));
        }
        // 倒序修改原有 w:t，兼容 Word 将同一个标记拆成多个 run 的情况。
        // 不删除 run，标记外的字体、上下标、图片和域保持原样。
        for (int i = ranges.size() - 1; i >= 0; i--)
        {
            int start = ranges.get(i)[0];
            int end = ranges.get(i)[1];
            for (int j = texts.size() - 1; j >= 0; j--)
            {
                Element element = texts.get(j);
                int offset = offsets.get(j);
                String value = element.getTextContent();
                if (offset >= end || offset + value.length() <= start)
                {
                    continue;
                }
                int from = Math.max(0, start - offset);
                int to = Math.min(value.length(), end - offset);
                String replacement = offset <= start ? replacements.get(i) : "";
                element.setTextContent(value.substring(0, from) + replacement + value.substring(to));
            }
        }
        // 同一 w:t 中可能有多个标记，全部替换后再拆换行，避免后续标记的偏移失效。
        if (!ranges.isEmpty())
        {
            for (Element element : texts)
            {
                setText(element, element.getTextContent());
            }
        }
    }

    private void setText(Element text, String value)
    {
        String[] lines = value.replace("\r\n", "\n").replace('\r', '\n').split("\n", -1);
        text.setTextContent(lines[0]);
        text.setAttributeNS(XMLConstants.XML_NS_URI, "xml:space", "preserve");
        Node next = text.getNextSibling();
        for (int i = 1; i < lines.length; i++)
        {
            Node run = text.getParentNode();
            run.insertBefore(text.getOwnerDocument().createElementNS(W, "w:br"), next);
            Element continuation = text.getOwnerDocument().createElementNS(W, "w:t");
            continuation.setAttributeNS(XMLConstants.XML_NS_URI, "xml:space", "preserve");
            continuation.setTextContent(lines[i]);
            run.insertBefore(continuation, next);
        }
    }

    private Object resolve(Map<String, Object> root, Object item, String path, int index)
    {
        if ("@序号".equals(path))
        {
            return index;
        }
        if (".".equals(path))
        {
            return item;
        }
        return path.startsWith(".") ? pathValue(item, path.substring(1)) : pathValue(root, path);
    }

    private boolean shouldRemoveRow(Node row, Map<String, Object> root, Object item, int index)
    {
        Matcher tokens = TOKEN.matcher(rowText(row));
        List<String> paths = new ArrayList<String>();
        List<String> primaryPaths = new ArrayList<String>();
        while (tokens.find())
        {
            String path = tokens.group(1);
            if (path.startsWith("#") || path.startsWith("@"))
            {
                continue;
            }
            if (path.startsWith("+"))
            {
                path = path.substring(1);
            }
            paths.add(path);
            if (isPrimaryDataPath(path))
            {
                primaryPaths.add(path);
            }
        }
        // 含“数值/含量”等核心字段的规格行，只由核心字段决定是否显示；
        // 单位、测试方法和类别等模板默认值不能单独让空行出现在导出文件中。
        List<String> candidates = primaryPaths.isEmpty() ? paths : primaryPaths;
        for (String path : candidates)
        {
            if (hasValue(root, item, path, index))
            {
                return false;
            }
        }
        return !paths.isEmpty();
    }

    private boolean hasValue(Map<String, Object> root, Object item, String path, int index)
    {
        return isMeaningful(resolve(root, item, path, index));
    }

    private boolean isPrimaryDataPath(String path)
    {
        String leaf = path;
        int dot = leaf.lastIndexOf('.');
        if (dot >= 0)
        {
            leaf = leaf.substring(dot + 1);
        }
        return "数值".equals(leaf) || "含量".equals(leaf) || "时间数值".equals(leaf)
                || "输出".equals(leaf) || "种类名".equals(leaf) || "成分名称".equals(leaf)
                || "元素名称".equals(leaf);
    }

    private boolean isMeaningful(Object value)
    {
        if (value == null)
        {
            return false;
        }
        if (value instanceof Iterable)
        {
            for (Object item : (Iterable<?>) value)
            {
                if (isMeaningful(item))
                {
                    return true;
                }
            }
            return false;
        }
        if (value instanceof Map)
        {
            for (Object item : ((Map<?, ?>) value).values())
            {
                if (isMeaningful(item))
                {
                    return true;
                }
            }
            return false;
        }
        return !display(value).trim().isEmpty();
    }

    private Object pathValue(Object value, String path)
    {
        if (!(value instanceof Map))
        {
            return null;
        }
        Map<?, ?> object = (Map<?, ?>) value;
        // 允许 CAS No. 等字段名本身包含句点。
        if (object.containsKey(path))
        {
            return object.get(path);
        }
        String prefix = null;
        for (Object key : object.keySet())
        {
            String name = String.valueOf(key);
            if (path.startsWith(name + ".") && (prefix == null || name.length() > prefix.length()))
            {
                prefix = name;
            }
        }
        return prefix == null ? null : pathValue(object.get(prefix), path.substring(prefix.length() + 1));
    }

    private List<?> items(Object value, boolean keepEmptyParagraph)
    {
        if (value instanceof List)
        {
            List<?> values = (List<?>) value;
            return values.isEmpty() && keepEmptyParagraph ? Collections.singletonList("") : values;
        }
        if (value == null || display(value).isEmpty())
        {
            return keepEmptyParagraph ? Collections.singletonList("") : Collections.emptyList();
        }
        return Arrays.asList(display(value).replace("\r\n", "\n").replace('\r', '\n').split("\n", -1));
    }

    private String display(Object value)
    {
        if (value == null)
        {
            return "";
        }
        // AI 偶尔会把比较符号写成 HTML 实体；写入 OOXML 前还原为实际字符，
        // DOM 会负责把 XML 中需要转义的字符正确编码。
        return String.valueOf(value).replace("&lt;", "<").replace("&gt;", ">")
                .replace("&le;", "≤").replace("&ge;", "≥").replace("&quot;", "\"")
                .replace("&#39;", "'").replace("&nbsp;", " ").replace("&amp;", "&");
    }

    private String rowText(Node row)
    {
        StringBuilder result = new StringBuilder();
        for (Node cell : children(row))
        {
            if (is(cell, "tc"))
            {
                for (Node paragraph : children(cell))
                {
                    if (is(paragraph, "p"))
                    {
                        result.append(paragraphText(paragraph));
                    }
                }
            }
        }
        return result.toString();
    }

    private String paragraphText(Node paragraph)
    {
        StringBuilder result = new StringBuilder();
        for (Element text : textNodes(paragraph))
        {
            result.append(text.getTextContent());
        }
        return result.toString();
    }

    private List<Element> textNodes(Node paragraph)
    {
        List<Element> result = new ArrayList<Element>();
        collectText(paragraph, result);
        return result;
    }

    private void collectText(Node parent, List<Element> result)
    {
        for (Node child : children(parent))
        {
            if (is(child, "t"))
            {
                result.add((Element) child);
            }
            else if (!is(child, "p"))
            {
                collectText(child, result);
            }
        }
    }

    private List<Node> children(Node parent)
    {
        List<Node> result = new ArrayList<Node>();
        for (Node child = parent.getFirstChild(); child != null; child = child.getNextSibling())
        {
            result.add(child);
        }
        return result;
    }

    private boolean is(Node node, String name)
    {
        return W.equals(node.getNamespaceURI()) && name.equals(node.getLocalName());
    }

    private byte[] readBytes(InputStream input) throws Exception
    {
        ByteArrayOutputStream result = new ByteArrayOutputStream();
        byte[] buffer = new byte[8192];
        int count;
        while ((count = input.read(buffer)) != -1)
        {
            result.write(buffer, 0, count);
        }
        return result.toByteArray();
    }
}
