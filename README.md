# ChatNexus

ChatNexus is a robust chat platform built with Java and Netty’s asynchronous, event‑driven framework, capable of handling hundreds of concurrent connections with minimal latency.  
It delivers real‑time messaging—group chat, direct messages, and simple menu‑driven controls—for seamless user interaction.  
A Gemini LLM–powered chatbot is integrated so users always have someone (or something) to chat with, even when no other participants are online.

---
## Features

- **Group chat** – broadcast to everyone online  
- **Direct messages** – one‑to‑one conversations with username validation  
- **Menu commands** – `/dm`, `/users`, `/menu`, `/quit`, etc.  
- **Gemini chatbot** – AI companion available 24/7  
- **Graceful disconnect handling** – DM partner returns to the main menu if the other side leaves  

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
