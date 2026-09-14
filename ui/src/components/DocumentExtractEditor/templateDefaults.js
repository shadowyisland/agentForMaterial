const measurement = (unit, fields) =>
  Object.assign({}, fields || {}, {
    单位: unit,
    数值: "",
  });

const viscosity = (method, speed) =>
  measurement("mPa·s", {
    测试温度: "@ 25°C",
    测试方法: method || "Brookfield DVNXHBCP, XXX",
    测试转速: speed || "@ XXXrpm",
  });

const acrylic = () => ({
  产品描述: "",
  主要特点: [],
  产品规格: {
    化学类型: "",
    外观: "",
    粘度: viscosity("Brookfield DVDV, SP 14#", "@ 20rpm"),
    酸值: measurement("mgKOH/g"),
    密度: measurement("g/cm3"),
    "折光率（液态）": {
      测试温度: "@ 25°C",
      数值: "",
    },
    有效成分: measurement("%"),
    官能度: "",
    断裂伸长率: measurement("%"),
    硬度: measurement("shore D"),
    色度: measurement("APHA/Pt-Co", {
      测试标准: "ASTM D1209",
    }),
    水分含量: measurement("ppm"),
    残留单体含量: measurement("%"),
    阻聚剂含量: measurement("ppm"),
    "数均分子量（Mn）": measurement("g/mol"),
    "重均分子量（Mw）": measurement("g/mol"),
    "分子量分布（Mw/Mn）": "",
    "玻璃化转变温度（Tg）": measurement("°C"),
    热分解温度: measurement("°C"),
    "C=C双键当量（不饱和度）": measurement("g/eq"),
    固化方式: {
      固化条件: "热固化/UV/双重固化",
      温度: "@ 150°C",
      时间单位: "分钟",
      时间数值: "30",
    },
  },
  推荐用途: "",
  储存及贮存期: "",
  安全及健康: "",
  可用包装: "",
  免责声明: "",
});

const epoxy = () => ({
  主要特性: "",
  典型参数: {
    外观: "",
    色相: "",
    粘度: viscosity(),
    官能团当量: measurement("g/eq"),
    可水解氯: measurement("ppm"),
    无机氯: measurement("ppm"),
    总氯: measurement("ppm"),
    不挥发物: measurement("%"),
    分子量: "",
    折射率: "",
    密度: measurement("g/cm3"),
    含水量: measurement("%"),
    色度: measurement("APHA"),
  },
  使用方法: "",
  主要应用: "",
  包装: "",
  贮存及保质期: "",
  注意事项: "",
  安全及健康: "",
  特别说明: "",
});

const otherResin = () => ({
  组分及结构式: "",
  主要特性: [],
  物化参数: {
    化学类型: "",
    外观: "",
    物理形态: "",
    固含: measurement("%"),
    纯度: measurement("%"),
    苯乙烯含量: measurement("wt%"),
    密度: measurement("kg/m3", {
      测试温度: "@ 25°C",
    }),
    "硬度Type A": {
      数值: "",
    },
    分子量: measurement(""),
    吸水率: measurement("%"),
    热变形温度: measurement("℃"),
    软化点: measurement("℃"),
    玻璃化转变温度Tg: measurement("℃"),
    连续使用温度: measurement("℃"),
    线性热膨胀系数: measurement("cm/cm/℃"),
    体积电阻率: measurement("Ω·cm"),
    表面电阻率: measurement("Ω"),
    "100%模量": measurement("MPa"),
    拉伸强度: measurement("MPa"),
    拉伸模量: measurement("MPa"),
    断裂伸长率: measurement("%"),
    弯曲模量: measurement("MPa"),
    弯曲强度: measurement("MPa"),
    悬臂梁缺口冲击强度: measurement("J/m"),
    "熔体流动速率（MFR）@230℃ 2.16kg": measurement("g/10min"),
    "熔体流动速率（MFR）@200℃ 10kg": measurement("g/10min"),
    "粘度（190℃）": measurement("mPa·s"),
    "溶液粘度（5wt%甲苯溶液）": viscosity(),
    "溶液粘度（10wt%甲苯溶液）": viscosity(),
    "溶液粘度（15wt%甲苯溶液）": viscosity(),
    UL94阻燃等级: "",
    加工收缩率: measurement("%"),
  },
  应用: "",
  包装: "",
  贮存及运输: "",
  安全及健康: "",
  特别说明: "",
});

const filler = () => ({
  化学成分: [
    {
      成分名称: "",
      标准值: measurement("%"),
    },
    {
      成分名称: "",
      标准值: measurement("%"),
    },
  ],
  主要特性: "",
  物化参数: {
    化学类型: "",
    外观: "",
    白度: measurement("%"),
    黑度: measurement("%"),
    粒度: [
      {
        类别: "D10",
        单位: "μm",
        数值: "",
      },
      {
        类别: "D50",
        单位: "μm",
        数值: "",
      },
      {
        类别: "D90",
        单位: "μm",
        数值: "",
      },
      {
        类别: "D100",
        单位: "μm",
        数值: "",
      },
    ],
    比表面积: measurement("m²/g"),
    片材尺寸: [
      {
        类别: "片径",
        单位: "μm",
        数值: "",
      },
      {
        类别: "片厚",
        单位: "nm",
        数值: "",
      },
      {
        类别: "长径比",
        单位: "",
        数值: "",
      },
    ],
    球化率: measurement("%"),
    椭圆率: "",
    密度: [
      {
        类别: "真密度",
        单位: "g/cm3",
        数值: "",
      },
      {
        类别: "振实密度",
        单位: "nm",
        数值: "",
      },
      {
        类别: "堆积密度",
        单位: "g/cm3或g/L",
        数值: "",
      },
    ],
    含水率: measurement("%"),
    挥发分: measurement("%"),
    热导率: measurement("W/(m·K)"),
    电导率: measurement("μS/cm"),
    含碳率: measurement("%"),
    其他元素含量: [
      {
        元素名称: "",
        单位: "ppm 或 %",
        含量: "",
      },
    ],
    吸油值: measurement("%"),
    折射率: "",
    "溶剂/分散剂": [
      {
        类别: "种类",
        种类名: "",
        单位: "",
        数值: "",
      },
      {
        类别: "含量",
        种类名: "",
        单位: "%",
        数值: "",
      },
    ],
    载体: "",
    pH: "",
    耐光性: "",
    耐迁移: "",
    着色力: measurement("%"),
    耐晒性: "",
    耐热性: measurement("°C"),
    耐酸性: "",
    耐碱性: "",
    油渗性: "",
    水渗性: "",
  },
  应用: "",
  包装: "",
  贮存及运输: "",
  安全及健康: "",
  特别说明: "",
});

const silicone = () => ({
  组分及结构式: "",
  主要特性: [],
  物化参数: {
    化学类型: "",
    外观: "",
    粘度: viscosity(),
    挥发分: measurement("%", {
      测试条件: "@ XXX°C Xh",
    }),
    比重: measurement("g/cm3", {
      测试温度: "@ 25°C",
    }),
    折射率: {
      测试温度: "@ 25°C",
      数值: "",
    },
    纯度: measurement("%"),
    "D3-D10": measurement("ppm"),
    乙烯基含量: measurement("mmol/g"),
    硅氢含量: measurement("mmol/g"),
    苯基含量: measurement("mol%"),
    官能团当量: measurement("g/mol", {
      官能团: "@ 羟基、氨基等",
    }),
    闪点: measurement("°C", {
      测试方法: "@ 闭杯",
    }),
    "玻璃化转变温度 Tg": measurement("°C"),
  },
  应用: "",
  包装: "",
  贮存及运输: "",
  安全及健康: "",
  特别说明: "",
});

const additive = () => ({
  组分及结构式: "",
  主要特性: "",
  物化参数: {
    化学类型: "",
    外观: "",
    活性成份占比: measurement("%"),
    粘度: viscosity(),
    挥发份: measurement("%", {
      测试条件: "@ XXX°C Xh",
    }),
    比重: measurement("g/cm3", {
      测试温度: "@ 25°C",
    }),
    溶剂: "",
    PH值: "",
    酸值: measurement("mgKOH/g"),
    胺值: measurement("mgKOH/g"),
    羟值: measurement("mgKOH/g"),
    "环氧当量/环氧值": measurement(""),
    异氰酸酯基含量: measurement("NCO%"),
    VOC: measurement("ppm"),
    闪点: measurement("°C"),
    水含量: measurement("%"),
    "沸点/馏程": measurement("°C"),
    "熔点/软化点/倾点": measurement("°C"),
    玻璃化转变温度: measurement("°C"),
    "折射率（nD,25°C）": {
      数值: "",
    },
    表面张力: measurement("mN/m"),
    粒径及粒径分布: measurement("μm"),
    HLB值: {
      数值: "",
    },
  },
  推荐应用及适用类型: "",
  建议用量: "",
  加入方法及加工指导: "",
  包装: "",
  贮存及运输: "",
  安全及健康: "",
  特别说明: "",
});

const msds = () => ({
  产品中文名称: "",
  产品英文名称: "",
  产品编号: "",
  "CAS No.": "",
  "EC No.": "",
  分子式: "",
  REACH注册号: "",
  UFI: "",
  "第1部分 物质或混合物和供应商的标识": {
    产品的推荐用途: "",
    产品的限制用途: "",
    企业名称: "",
    企业地址: "",
    邮编: "",
    联系电话: "",
    电子邮箱: "",
    应急电话: "",
    响应时间: "",
  },
  "第2部分 危险概述": {
    危险性分类: "",
    象形图: "",
    信号词: "",
    危险性说明: "",
    防范说明: "",
    补充危险信息: "",
  },
  "第3部分 成分/组成信息": {
    物质或混合物: "",
    成分: [
      {
        成分名称: "",
        "CAS No.": "",
        "EC No.": "",
        "Index No.": "",
        含量: "",
        单位: "%",
        危险性分类: "",
        特定浓度限值和M因子: "",
      },
    ],
  },
  "第4部分 急救措施": {
    一般性建议: "",
    眼睛接触: "",
    皮肤接触: "",
    食入: "",
    吸入: "",
    急救人员的防护: "",
    最重要的急性和延迟症状: "",
    紧急医疗处理: "",
  },
  "第5部分 消防措施": {
    适当的灭火介质: "",
    不适当的灭火介质: "",
    特别危害: "",
    对消防人员的建议: "",
  },
  "第6部分 泄漏应急处理": {
    作业人员防护措施: "",
    环境保护措施: "",
    清洁方法和材料: "",
  },
  "第7部分 操作处置与储存": {
    安全操作的防护措施: "",
    安全储存条件: "",
    特定用途: "",
  },
  "第8部分 接触控制/个体防护": {
    控制参数: "",
    工程控制: "",
    眼睛防护: "",
    手部防护: "",
    呼吸系统防护: "",
    皮肤和身体防护: "",
  },
  "第9部分 物理和化学特性": {
    物理状态: "",
    颜色: "",
    气味: "",
    pH值: "",
    熔点或凝固点: "",
    沸点: "",
    闪点: "",
    蒸气压: "",
    相对密度: "",
    溶解性: "",
    运动粘度: "",
  },
  "第10部分 稳定性和反应性": {
    反应性: "",
    化学稳定性: "",
    危险反应的可能性: "",
    避免接触的条件: "",
    禁配物: "",
    危险的分解产物: "",
  },
  "第11部分 毒理学信息": {
    急性毒性: "",
    皮肤腐蚀或刺激: "",
    严重眼损伤或刺激: "",
    呼吸或皮肤致敏: "",
    致癌性: "",
    生殖毒性: "",
    吸入危害: "",
  },
  "第12部分 生态学信息": {
    急性水生毒性: "",
    慢性水生毒性: "",
    持久性和降解性: "",
    生物富集: "",
    土壤迁移性: "",
  },
  "第13部分 废弃处置": {
    废弃化学品: "",
    污染包装物: "",
    废弃注意事项: "",
  },
  "第14部分 运输信息": {
    UN编号: "",
    正确运输名称: "",
    运输主要危险类别: "",
    运输次要危险类别: "",
    包装类别: "",
    海洋污染物: "",
  },
  "第15部分 法规信息": {
    法规信息: "",
  },
  "第16部分 其他信息": {
    编制日期: "",
    修订日期: "",
    修订原因: "",
    免责声明: "",
  },
});

const factories = {
  ACRYLIC: acrylic,
  EPOXY: epoxy,
  OTHER_RESIN: otherResin,
  FILLER: filler,
  SILICONE: silicone,
  ADDITIVE: additive,
};

export const DELETED_ROWS_KEY = "__删除行";

export function createExtractTemplate(materialCategory, documentKind) {
  return documentKind === "MSDS"
    ? msds()
    : factories[materialCategory]
    ? factories[materialCategory]()
    : {};
}

export function mergeExtractTemplate(template, values) {
  // MSDS 提示词本身就是与 Word 模板一致的完整结构。旧的前端默认结构仍是早期
  // “危险概述/扁平急救措施”版本，会覆盖新提示词的数组对象，造成 AI 有值而页面为空。
  if (values && Object.prototype.hasOwnProperty.call(values, "第2部分 危险标识")) {
    const result = JSON.parse(JSON.stringify(values));
    if (!Array.isArray(result.图片)) result.图片 = [];
    return result;
  }
  const result = mergeTemplate(template, values);
  // 助剂、填料的“主要特性”为单段文本；旧记录可能仍是数组，合并时保留内容并转为多行文本。
  if (template.主要特性 === "" && Array.isArray(result.主要特性)) {
    result.主要特性 = result.主要特性.filter(Boolean).join("\n");
  }
  const deletedRows =
    values && Array.isArray(values[DELETED_ROWS_KEY])
      ? values[DELETED_ROWS_KEY]
      : [];
  deletedRows.forEach((path) => removeByPath(result, path));
  if (deletedRows.length) result[DELETED_ROWS_KEY] = deletedRows;
  return result;
}

function mergeTemplate(template, values) {
  if (Array.isArray(template)) {
    if (!Array.isArray(values)) return template;
    const hasCategories =
      template.length &&
      template.every((item) => item && typeof item === "object" && item.类别);
    if (!hasCategories) return values;
    // 已保存的数组决定行数和顺序，不恢复用户主动删除的预置行。
    return values.map((item) => {
      const sample = template.find((value) => item && value.类别 === item.类别);
      return sample ? mergeTemplate(sample, item) : item;
    });
  }
  if (template && typeof template === "object") {
    const result = {};
    const source = values && typeof values === "object" ? values : {};
    Object.keys(template).forEach((key) => {
      result[key] = mergeTemplate(template[key], source[key]);
    });
    // MSDS 使用完整的通用提示词结构；保留模版中尚未预置的字段，避免打开表单后丢失解析结果。
    Object.keys(source).forEach((key) => {
      if (!Object.prototype.hasOwnProperty.call(result, key)) result[key] = source[key];
    });
    return result;
  }
  return values === undefined || values === null ? template : values;
}

function removeByPath(object, path) {
  const keys = String(path || "").split(".").filter(Boolean);
  if (!keys.length) return;
  let parent = object;
  keys.slice(0, -1).forEach((key) => {
    parent = parent && parent[key];
  });
  if (parent && typeof parent === "object") delete parent[keys[keys.length - 1]];
}
