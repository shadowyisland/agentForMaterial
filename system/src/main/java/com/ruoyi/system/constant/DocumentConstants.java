package com.ruoyi.system.constant;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * 文档类型、材料分类与检索范围常量。
 */
public final class DocumentConstants
{
    public static final String TYPE_INTERNAL = "INTERNAL";
    public static final String TYPE_EXTERNAL = "EXTERNAL";

    public static final String CATEGORY_RAW_MATERIAL = "RAW_MATERIAL";

    public static final String KIND_TDS = "TDS";
    public static final String KIND_MSDS = "MSDS";

    public static final String SEARCH_ALL = "all";
    public static final String SEARCH_OCR = "ocr";
    public static final String SEARCH_RECORD = "record";

    public static final Set<String> MATERIAL_CATEGORIES = Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(
            CATEGORY_RAW_MATERIAL,
            "ACRYLIC",
            "EPOXY",
            "OTHER_RESIN",
            "FILLER",
            "SILICONE",
            "ADDITIVE"
    )));

    public static final Set<String> DOCUMENT_KINDS = Collections.unmodifiableSet(new HashSet<String>(Arrays.asList(
            KIND_TDS,
            KIND_MSDS
    )));

    private DocumentConstants()
    {
    }
}
