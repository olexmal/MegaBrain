# MegaBrain RAG Pipeline - Documentation

**Version:** 1.1.0  
**Last Updated:** February 2026

---

## What is MegaBrain?

MegaBrain is a **scalable, self-hosted, intelligent code knowledge platform** that indexes multi-language source code from various repositories and provides precise semantic search and natural language Q&A through a modern, reactive architecture.

### Core Value Proposition

MegaBrain solves the problem of **knowledge fragmentation** across large, polyglot, and actively evolving codebases. It moves beyond simple text search by understanding code semantics and structure, providing developers with instant, context-aware answers about their own code.

### Key Features

- **Semantic Code Search** - Find code by meaning, not just keywords
- **Natural Language Q&A** - Ask questions about your codebase in plain English
- **Multi-Language Support** - Java, Python, C/C++, JavaScript/TypeScript, Go, Rust, Kotlin, Ruby, Scala, Swift, PHP, C#
- **Dependency Graph Analysis** - Visualize and analyze code relationships
- **Documentation Intelligence** - Extract and correlate documentation from code
- **Privacy-First** - Fully self-hosted, supports offline operation with Ollama
- **High Performance** - Sub-second query latency, millions of lines indexed daily
- **Multiple Interfaces** - Web UI, CLI, REST API, and MCP Server

### Problem Statement

**The Problem:**
- **Lost Context:** Developers struggle to find specific implementations, API usage patterns, and documentation scattered across repositories
- **Inefficient Search:** Traditional `grep` or IDE search lacks semantic understanding and cannot answer "how-to" questions
- **External Dependency Risk:** Using general-purpose LLMs or external code assistants risks exposing proprietary code
- **Manual Overhead:** Onboarding new team members requires extensive, manual code traversal

**The Solution:**
MegaBrain creates a private, intelligent knowledge base of your codebase that enables semantic search and Q&A while keeping all data in-house.

---

## Documentation Index

| Document | Description |
|:---------|:------------|
| [Getting Started](getting-started.md) | Prerequisites, installation, and verification |
| [Architecture](architecture.md) | Component architecture, packages, and data flow |
| [Technology Stack](technology-stack.md) | Backend and frontend technologies with versions |
| [Implemented Features](implemented-features.md) | Detailed documentation for all completed features |
| [API Reference](api-reference.md) | REST API endpoints, parameters, and response formats |
| [Configuration Reference](configuration-reference.md) | All configuration properties with defaults |
| [Development Guide](development-guide.md) | Coding standards, testing, git workflow, contributing |
| [Deployment & Operations](deployment.md) | Production build, system requirements, troubleshooting |

---

## Quick Links

- **New to MegaBrain?** Start with the [Getting Started](getting-started.md) guide
- **Setting up configuration?** See the [Configuration Reference](configuration-reference.md)
- **Integrating with the API?** See the [API Reference](api-reference.md)
- **Want to contribute?** See the [Development Guide](development-guide.md)

---

## Project Documentation

- [Feature Specification](../features/feature_specification.md) - Complete feature specification
- [Epics Overview](../features/epics/README.md) - All epics and their relationships
- [User Stories Backlog](../features/user-stories/README.md) - User stories and sprint planning
- [User Setup Guide](../USER_SETUP.md) - Deployment and user configuration
- [Frontend README](../frontend/README.md) - Angular frontend documentation
- [Backend Benchmarks](../backend/BENCHMARKS.md) - Performance benchmarks

## External Resources

- [Quarkus Documentation](https://quarkus.io/guides/)
- [Angular Documentation](https://angular.io/docs)
- [Apache Lucene](https://lucene.apache.org/)
- [Neo4j Java Driver](https://neo4j.com/docs/java-manual/current/)
- [LangChain4j](https://github.com/langchain4j/langchain4j)
- [Tree-sitter](https://tree-sitter.github.io/tree-sitter/)

## Epic Documentation

- [EPIC-00: Project Infrastructure Setup](../features/epics/EPIC-00-project-infrastructure.md)
- [EPIC-01: Code Ingestion & Indexing](../features/epics/EPIC-01-ingestion.md)
- [EPIC-02: Hybrid Search & Retrieval](../features/epics/EPIC-02-search.md)
- [EPIC-03: RAG Answer Generation](../features/epics/EPIC-03-rag.md)
- [EPIC-04: REST API & CLI](../features/epics/EPIC-04-api-cli.md)
- [EPIC-05: Web Dashboard](../features/epics/EPIC-05-web-dashboard.md)
- [EPIC-06: Dependency Graph Analysis](../features/epics/EPIC-06-dependency-graph.md)
- [EPIC-07: Documentation Intelligence](../features/epics/EPIC-07-documentation.md)
- [EPIC-08: MCP Tool Server](../features/epics/EPIC-08-mcp-server.md)

---

## License

This project is licensed under the MIT License - see the [LICENSE](../LICENSE) file for details.

## Support

For issues, questions, or contributions:
- Open an issue on GitHub
- Check existing documentation
- Review epic and user story specifications

---

**Last Updated:** February 2026  
**Document Version:** 1.1.0
