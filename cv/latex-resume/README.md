# 潘少峰 LaTeX 简历

这是根据 `../小鹏汽车-潘少峰.pdf` 重建的可编辑 LaTeX 工程。默认使用 XeLaTeX，在 macOS 上使用系统自带的 Helvetica Neue 与冬青黑体简体中文字体。工程支持根据项目经历自然扩展为多页，不要求固定为一页。

项目协作、内容真实性和构建验证规范见 `AGENTS.md`。

## 编译

```bash
cd latex-resume
make
```

生成文件位于 `build/resume.pdf`。也可以直接运行：

```bash
latexmk -xelatex resume.tex
```

## 修改指南

- 姓名、电话、邮箱、求职方向、城市：修改 `resume.tex` 顶部“常用字段”。
- 个人优势：修改 `sections/summary.tex`。
- 工作经历：修改 `sections/experience.tex`。
- 项目经历：修改 `sections/projects.tex`。
- 教育经历：修改 `sections/education.tex`。
- 简历问题与优化待办：查看 `RESUME_TODO.md`。
- 面试热点与高概率问题：查看 `INTERVIEW_QUESTIONS.md`。
- 头像：替换 `assets/photo.png`；若不想显示，将 `resume.tex` 中的 `\showphototrue` 改为 `\showphotofalse`。
- 主题颜色与页边距：修改 `resume.tex` 中的 `Accent`、`Heading`、`Rule` 和 `geometry` 参数。

简历中的量化指标沿用原 PDF。投递前建议逐项确认数据口径，并根据目标岗位 JD 调整关键词。
