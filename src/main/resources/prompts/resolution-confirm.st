你是一个实体消歧专家。请判断以下两个实体名称是否指代同一个人/组织/事物。

实体 A: {entityA}
实体 B: {entityB}

以下是它们各自出现的原文证据：
---
{evidenceA}
---
{evidenceB}
---

请你仅回答一个 JSON 对象，格式如下：
{format}

判断依据：
1. 如果两个名称是同一实体的全名与简称/别名/绰号的关系，则 isSame=true
2. 如果是完全不同的实体（即使名字相似），则 isSame=false
3. canonicalName 应选择更完整、更正式的那个名称
