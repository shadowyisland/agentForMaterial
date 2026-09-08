package com.ruoyi.system.service;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.junit.Test;

public class DocumentWordTemplateRendererTest
{
    private static final String PREFIX = "<w:document xmlns:w=\"http://schemas.openxmlformats.org/wordprocessingml/2006/main\"><w:body>";
    private static final String SUFFIX = "</w:body></w:document>";

    @Test
    public void removesStaticSpecificationRowWhenOnlyDefaultUnitExists() throws Exception
    {
        String xml = PREFIX
                + "<w:tbl><w:tr><w:tc><w:p><w:r><w:t>粘度</w:t></w:r></w:p></w:tc>"
                + "<w:tc><w:p><w:r><w:t>{{规格.粘度.数值}}</w:t></w:r></w:p></w:tc>"
                + "<w:tc><w:p><w:r><w:t>{{规格.粘度.单位}}</w:t></w:r></w:p></w:tc></w:tr></w:tbl>"
                + SUFFIX;
        Map<String, Object> viscosity = map("数值", "", "单位", "mPa·s");
        String rendered = render(xml, map("规格", map("粘度", viscosity)));

        assertFalse(rendered.contains("粘度"));
        assertFalse(rendered.contains("mPa·s"));
    }

    @Test
    public void filtersEmptyTemplateArrayRowsButKeepsRowsWithValues() throws Exception
    {
        String xml = PREFIX
                + "<w:tbl><w:tr><w:tc><w:p><w:r><w:t>{{#粒度}}{{.类别}}</w:t></w:r></w:p></w:tc>"
                + "<w:tc><w:p><w:r><w:t>{{.数值}}</w:t></w:r></w:p></w:tc>"
                + "<w:tc><w:p><w:r><w:t>{{.单位}}</w:t></w:r></w:p></w:tc></w:tr></w:tbl>"
                + SUFFIX;
        Map<String, Object> empty = map("类别", "D10", "数值", "", "单位", "μm");
        Map<String, Object> filled = map("类别", "D50", "数值", "12.5", "单位", "μm");
        String rendered = render(xml, map("粒度", Arrays.asList(empty, filled)));

        assertFalse(rendered.contains("D10"));
        assertTrue(rendered.contains("D50"));
        assertTrue(rendered.contains("12.5"));
    }

    @Test
    public void removesEmptyMultilinePlaceholderParagraph() throws Exception
    {
        String xml = PREFIX
                + "<w:p><w:r><w:t>{{+产品描述}}</w:t></w:r></w:p>"
                + "<w:p><w:r><w:t>固定内容</w:t></w:r></w:p>" + SUFFIX;
        String rendered = render(xml, map("产品描述", ""));

        assertFalse(rendered.contains("产品描述"));
        assertFalse(rendered.contains("{{+"));
        assertTrue(rendered.contains("固定内容"));
    }

    private String render(String documentXml, Map<String, Object> values) throws Exception
    {
        ByteArrayOutputStream template = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(template))
        {
            zip.putNextEntry(new ZipEntry("word/document.xml"));
            zip.write(documentXml.getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
        }
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        new DocumentWordTemplateRenderer().render(new ByteArrayInputStream(template.toByteArray()), values, output);
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(output.toByteArray())))
        {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null)
            {
                if ("word/document.xml".equals(entry.getName()))
                {
                    ByteArrayOutputStream content = new ByteArrayOutputStream();
                    byte[] buffer = new byte[1024];
                    int count;
                    while ((count = zip.read(buffer)) != -1)
                    {
                        content.write(buffer, 0, count);
                    }
                    return content.toString(StandardCharsets.UTF_8.name());
                }
            }
        }
        throw new AssertionError("rendered document.xml not found");
    }

    private Map<String, Object> map(Object... values)
    {
        Map<String, Object> result = new HashMap<String, Object>();
        for (int i = 0; i < values.length; i += 2)
        {
            result.put(String.valueOf(values[i]), values[i + 1]);
        }
        return result;
    }
}
