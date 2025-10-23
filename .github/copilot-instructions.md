# Copilot Instructions for My-Note-Book

## Repository Overview

This is a Chinese/English bilingual personal knowledge repository containing technical notes, interview preparation materials, CV documents, and mathematical derivations. The content spans software engineering (Java, architecture components), computer science fundamentals, career development, and mathematics.

## Content Structure & Conventions

### Directory Organization
- `java/` - Java ecosystem notes (collections, concurrency, JVM)
- `常见架构组件/` - Common architecture components (MySQL, Redis, Kafka, Zookeeper, etc.)
- `spring/` - Spring Framework and Spring Cloud notes
- `computer-science/` - CS fundamentals (CPU cache, copy-on-write)
- `数学/` - Mathematics notes with LaTeX documents (probability, calculus, finance)
- `cv/` - CV materials (both LaTeX source in `cv_curve_1/` and Markdown notes)
- `prompt/` - AI prompt engineering templates
- `personal/` - Personal career planning and industry research
- `tour_list/` - Personal travel planning

### Documentation Patterns

**Question-Driven Structure**: Most technical documents follow a Q&A format starting with "什么是...?" (What is...?) and "常见问题" (Common Questions) sections. Example from `java/HashMap.md`:
```markdown
# 1. 是什么
# 2. 什么使用场景  
# 3. 什么核心机制
# 4. 解决什么问题（什么特点）
# 5. 常见问题有哪些
```

**Bilingual Content**: Many documents provide both English and Chinese explanations, especially in `常见架构组件/` files. Technical terms often include English in parentheses.

**Mathematical Content**: Files in `数学/` use KaTeX/LaTeX syntax for formulas. LaTeX source files (`.tex`) exist alongside compiled outputs (`.aux`, `.pdf`, `.dvi`).

**Cross-referencing**: Documents link to related notes using relative paths, e.g., `[哈希算法](../算法/Hash算法.md)`.

## Working with Different Content Types

### Technical Notes (Java/Architecture)
- Maintain the Q&A structure when adding content
- Include both Chinese explanations and English technical terms
- Add code examples in markdown code blocks with language tags
- Reference official documentation URLs when available

### CV Documents
- LaTeX source is in `cv/cv_curve_1/cv-llt.tex` using the `curve` document class
- Individual sections split into separate `.tex` files: `education.tex`, `employment.tex`, `experience.tex`, `skills.tex`, `publications.tex`
- Markdown notes in `cv/项目经历.md` follow STAR principle (Situation, Task, Action, Result)
- When updating CV, modify both LaTeX source and Markdown notes for consistency

### Mathematical Content
- Use KaTeX syntax for inline math `$...$` and display math `$$...$$` in Markdown
- LaTeX documents use UTF8 encoding with `\usepackage[UTF8]{ctex}` for Chinese support
- Probability theory content is in `数学/概率论/`, with both `.md` and `.tex` formats

### Prompt Engineering Files
- `prompt/prompt_mother.md` contains a meta-prompt for generating prompts
- Structure: Expert persona → Consultation process → Final output
- Design for iterative refinement through Q&A dialogue

## Development Workflows

### Building LaTeX Documents
The repository includes `.tex` files but no automated build scripts. To compile:
```powershell
# For CV documents (requires XeLaTeX for Chinese support)
cd cv\cv_curve_1
xelatex cv-llt.tex
biber cv-llt      # If bibliography exists
xelatex cv-llt.tex
```

### Markdown Editing
- Files use standard Markdown with KaTeX math extensions
- No build process required - rendered directly by Obsidian or VS Code
- `.obsidian/` directory suggests Obsidian is the primary Markdown editor

## Project-Specific Conventions

1. **Empty Files**: Several files exist as placeholders (e.g., `java/JAVA线程池.md`, `常见架构组件/Redis.md`) - these indicate topics planned but not yet documented

2. **Naming**: 
   - Chinese filenames for content-focused documents
   - Pinyin or English for technical components
   - Mixed naming is intentional and should be preserved

3. **File Artifacts**: LaTeX compilation artifacts (`.aux`, `.bbl`, `.blg`, `.fls`, `.fdb_latexmk`, `.run.xml`) are committed - when regenerating LaTeX, preserve these for version tracking

4. **Architecture Component Pattern**: Each component has two files:
   - `<Component>.md` - Core concepts and usage
   - `<Component>常见问题.md` - Interview-style Q&A
   
5. **Career Documentation**: `cv/项目经历.md` serves as source material for both CV generation and interview preparation, emphasizing STAR methodology and specific metrics (e.g., "性能提升了 40%")

## Key Integration Points

- **Obsidian Integration**: This is an Obsidian vault (`.obsidian/` directory present)
- **Git Version Control**: Changes tracked in `.git/`, use conventional commits when possible
- **LaTeX Toolchain**: Requires XeLaTeX/LuaLaTeX for Chinese support in mathematical and CV documents
- **No External Dependencies**: Pure markdown/LaTeX repository, no package.json or requirements.txt

## When Adding New Content

1. Follow existing directory structure based on topic domain
2. For technical topics, use the Q&A pattern with bilingual content
3. For math content, provide both intuitive explanation and formal derivation
4. Cross-reference related notes using relative markdown links
5. If creating placeholder files, commit empty files to signal planned content
