# ChatNexus

ChatNexus is a robust Java and Netty–based chat platform that effortlessly handles hundreds of concurrent connections with minimal latency, delivering real‑time group and direct messaging through intuitive, menu‑driven controls—plus an integrated Gemini LLM chatbot ensures there’s always someone (or something) to converse with, even when no other users are online.

---
## Features

- **Group chat** – broadcast to everyone online  
- **Direct messages** – one‑to‑one conversations with username validation  
- **Menu commands** – `/dm`, `/users`, `/menu`, `/quit`, etc.  
- **Gemini chatbot** – AI companion available 24/7
---
## Prerequisites

- **Java 8 or higher** – ensure `java` and `javac` are on your `PATH`  
- *(Optional)* **Maven** – only required if you want to build from source  
---
## Quick Start

1. **Download** the latest release ZIP.  
2. **Unzip** it (the pre‑built JAR and dependencies are already in `target/`).  

### Start the server
java -cp "target\ChatNexus-1.0-SNAPSHOT.jar;target\dependency\*" com.example.chat.Server 8080
### Start the client
java -cp "target\ChatNexus-1.0-SNAPSHOT.jar;target\dependency\*" com.example.chat.Client 127.0.0.1 8080
