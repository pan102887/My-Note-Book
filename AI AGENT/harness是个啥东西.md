# harness

## 什么是 harness? 它和平时使用的 skill、spec-driven develop、agent 有什么关系? 以及有什么区别?

### 1. 是什么

Harness(运行框架/控制框架)可以理解为: 为 Agent 设计的一套可执行运行环境与控制回路。

它不只是一个 prompt，也不只是一个 agent 实例，而是把下面这些能力编排到一起:

- 任务分解与计划(Planning)
- 文件系统/代码库操作(Filesystem)
- 工具执行与测试(Tooling + Execution)
- 上下文管理(Context management)
- 评估与反馈(Evaluation loop)
- 人类审批或中断(Human-in-the-loop)
- 持久记忆与技能加载(Memory + Skills)

一句话: harness engineering 的目标是让 Agent 在长时任务中可持续地产生高质量结果，而不是在单轮对话里“看起来能做”。

### 2. 演化路径

一个常见演化路径:

- Prompt Engineering: 优化单次指令质量。
- Context Engineering: 优化上下文注入、检索、压缩和结构化。
- Harness Engineering: 在前两者基础上，增加多阶段流程、角色分工、状态工件、自动测试、评估闭环和治理机制。

直观理解:

- Prompt 是“说清楚要做什么”。
- Context 是“给够做事所需信息”。
- Harness 是“让系统长期稳定地把事情做完并做对”。

### 3. 为什么要引入 harness? 它解决了什么问题?

在长任务(几小时到几天)中，常见失败模式:

- 一次性做太多，最后半成品堆积(one-shot overreach)
- 上下文变长后失焦，甚至提前宣布完成(context anxiety / premature closure)
- 自评偏乐观，尤其在设计类主观任务中“自我表扬”
- 会改代码但缺少端到端验证，导致核心链路实际上不可用

Harness 通过结构化机制来解决:

- 增量开发: 一次只推进一个可验证单元(feature/sprint)
- 状态交接: 用进度文件、特性清单、git 历史做 handoff artifact
- 角色分离: 生成者(generator)与评估者(evaluator)分离，降低自评偏差
- 真实验证: 通过浏览器自动化/接口测试/环境脚本做 E2E 验证
- 上下文治理: compaction + context reset(按模型能力选择)

### 4. 核心架构模式

#### 模式 A: Initializer + Coding Agent(Anthropic 早期长任务实践)

- Initializer Agent
  - 初始化仓库运行环境(如 `init.sh`)
  - 生成结构化特性清单(通常 JSON，防止被随意改写)
  - 建立进度日志文件(如 `progress`)
  - 做首个基线提交，确保后续可回滚
- Coding Agent(循环)
  - 每次只选一个未完成特性
  - 实现 + 测试 + 提交
  - 更新进度与特性状态

这个模式重点是跨 context window 的连续性与可恢复性。

#### 模式 B: Planner + Generator + Evaluator(Anthropic 后续扩展)

- Planner
  - 把 1-4 句高层需求扩展成完整产品 spec
  - 侧重范围和验收目标，避免过早锁死实现细节
- Generator
  - 按 feature 或 sprint 开发，产出代码和工件
- Evaluator(QA/Reviewer)
  - 使用工具(如 Playwright)进行真实交互测试
  - 对照标准打分并输出缺陷与改进意见

关键点是 Generator 与 Evaluator 分离，形成类似 GAN 的“生成-评估”迭代回路。

### 5. 关键机制: compaction vs context reset

- Compaction(压缩)
  - 在同一 agent 会话中压缩历史上下文，保持连续性
  - 优点: 开销较低，流程连续
  - 风险: 某些模型在超长任务仍会焦虑或失稳
- Context Reset(重置)
  - 清空上下文，启动新 agent，并依赖 handoff artifact 续作
  - 优点: 给模型“干净上下文”，降低长程漂移
  - 成本: 编排复杂度、token 和时延增加

实践建议:

- 如果模型长程稳定性较强，优先 compaction。
- 如果出现明显长任务漂移/提前收尾，加入 reset + 强化交接工件。

### 6. harness 的组成能力(结合 LangChain Deep Agents)

典型 harness 能力面板:

- Planning: `write_todos` 等任务计划工具
- Virtual Filesystem: `ls/read_file/write_file/edit_file/glob/grep`
- Task Delegation: subagent 任务委派和并行
- Context Management: 自动压缩、隔离、长程记忆
- Code Execution: sandbox 中 `execute`
- Human-in-the-loop: `interrupt_on` 审批拦截
- Skills: 按需加载领域技能
- Memory: 持久化约束与偏好(如 `AGENTS.md` 风格记忆)

这说明 harness 不是某个特定框架的专属概念，而是一组可组合的工程能力。

### 7. 它与 skills、agents、spec-driven coding 的关系与区别

#### 和 Agent 的关系

- Agent 是“执行者实例”(一个模型 + 一组工具 + 当前上下文)。
- Harness 是“执行系统设计”(定义多个 agent 如何协作、交接、评估、治理)。

类比:

- Agent 像工人。
- Harness 像生产线和质量体系。

#### 和 Skills 的关系

- Skill 是可复用的领域能力包(指令 + 资源 +脚本)。
- Harness 决定何时、由谁、在什么阶段调用哪些 skill。

换句话说，Skill 是“能力模块”，Harness 是“调度与控制层”。

#### 和 Spec-driven Development 的关系

- Spec-driven 强调: 先定义需求与验收，再实现。
- Harness 通常把 spec-driven 变为可自动执行流程:
  - planner 产出 spec
  - generator 依据 spec 迭代实现
  - evaluator 按验收标准验证

所以 spec-driven 更像方法论，harness 是让该方法论在 agent 系统里落地的工程外壳。

### 8. 可直接落地的最小实践清单

1. 初始化阶段
   - 生成 `init.sh`(一键启动与基础检查)
   - 生成 `feature_list.json`(结构化验收项，含 `passes` 字段)
   - 生成 `progress.md`(每轮决策、风险、下一步)
2. 开发循环
   - 每轮只做一个 feature 或一个 sprint
   - 强制 E2E 验证后再改状态
   - git 小步提交，提交信息包含“做了什么+如何验证”
3. 评估机制
   - 单独 evaluator，不与 generator 共享“自评结论”
   - 建立分维度评分(功能、质量、设计、可靠性)
   - 设硬阈值，任何一项不达标都回退修复
4. 上下文治理
   - 先用 compaction
   - 一旦出现长任务漂移，切 reset + handoff artifact
5. 安全与治理
   - 对高风险工具设置 human approval
   - 用 sandbox 执行命令
   - 对文档新鲜度和结构做 lint/CI 检查

### 9. 常见反模式

- 只有一个超长 `AGENTS.md`，试图把所有规则塞进去
- 只看单次 demo 效果，不做长时回归验证
- 让模型自评并自我放行，无独立 QA 角色
- feature 状态用自由文本记录，导致可验证性差
- 把 harness 当“固定模板”，不随模型能力升级而简化/重构

### 10. 一个建议目录(可选)

```text
AGENTS.md                   # 简短地图(不是百科全书)
ARCHITECTURE.md
docs/
  design-docs/
  exec-plans/
  product-specs/
  references/
harness/
  init.sh
  feature_list.json
  progress.md
  qa_criteria.md
```

核心原则: 给 agent 一张可导航的地图，而不是一坨不可维护的大说明书。

### 11. 一句话总结

Harness engineering 的本质是: 用工程化控制回路把“模型能力”转化为“稳定交付能力”。

### 12. 参考资料

- [Effective harnesses for long-running agents](https://www.anthropic.com/engineering/effective-harnesses-for-long-running-agents)
- [Harness design for long-running application development](https://www.anthropic.com/engineering/harness-design-long-running-apps)
- [工程技术：在智能体优先的世界中利用 Codex](https://openai.com/zh-Hans-CN/index/harness-engineering/)
- [LangChain Deep Agents: Harness capabilities](https://docs.langchain.com/oss/python/deepagents/harness)
