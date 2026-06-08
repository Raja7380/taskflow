package com.taskflow.learn.springai;

/**
 * =====================================================================
 * SPRING AI — Integrating AI/LLMs with Spring Boot
 * =====================================================================
 *
 * WHAT IS SPRING AI?
 *   A Spring project that makes it easy to integrate AI capabilities
 *   (ChatGPT, Claude, Gemini, etc.) into your Spring Boot application.
 *
 *   Just like Spring Data JPA abstracts database access,
 *   Spring AI abstracts AI model access!
 *
 * WHY SPRING AI?
 *   Without Spring AI, you'd manually:
 *     - Build HTTP requests to OpenAI API
 *     - Parse JSON responses
 *     - Handle errors, retries, rate limits
 *     - Manage conversation history
 *   Spring AI handles all of this with clean abstractions.
 *
 * =====================================================================
 * KEY CONCEPTS:
 * =====================================================================
 *
 * 1. CHAT CLIENT — Talk to AI models
 *
 *    @Service
 *    public class AiService {
 *        private final ChatClient chatClient;
 *
 *        public AiService(ChatClient.Builder builder) {
 *            this.chatClient = builder.build();
 *        }
 *
 *        public String summarizeTask(String taskDescription) {
 *            return chatClient.prompt()
 *                .user("Summarize this task in 2 sentences: " + taskDescription)
 *                .call()
 *                .content();
 *        }
 *    }
 *
 * 2. PROMPT TEMPLATES — Reusable prompts with variables
 *
 *    @Value("classpath:prompts/summarize.st")
 *    private Resource promptResource;
 *
 *    // summarize.st file:
 *    // Summarize this {type} in {maxSentences} sentences: {content}
 *
 *    String result = chatClient.prompt()
 *        .user(u -> u.text(promptResource)
 *            .param("type", "task")
 *            .param("maxSentences", "2")
 *            .param("content", taskDescription))
 *        .call()
 *        .content();
 *
 * 3. RAG (Retrieval-Augmented Generation)
 *    WHY: LLMs don't know about YOUR project data.
 *    HOW: Feed relevant data to the LLM along with the question.
 *
 *    Flow:
 *      1. Store project documents as embeddings (vectors) in a vector DB
 *      2. User asks "Who's assigned to the login feature?"
 *      3. Search vector DB for relevant documents
 *      4. Send those documents + question to LLM
 *      5. LLM answers using YOUR data!
 *
 * 4. EMBEDDINGS — Convert text to numbers (vectors)
 *    "Hello world" -> [0.12, -0.34, 0.56, 0.78, ...]
 *    Similar texts have similar vectors (can measure distance)
 *    Used for: semantic search, recommendation, RAG
 *
 * 5. FUNCTION CALLING — Let AI trigger your Java methods
 *
 *    @Bean
 *    public Function<TaskRequest, TaskResponse> createTask() {
 *        return request -> taskService.createTask(request);
 *    }
 *
 *    // AI can now call createTask() when user says "Create a task for..."
 *
 * =====================================================================
 * HOW TO ADD SPRING AI TO TASKFLOW:
 * =====================================================================
 *
 * Feature 1: AI Task Summarization
 *   - User creates long task description
 *   - AI auto-generates a 2-line summary
 *   - Displayed in task cards on the dashboard
 *
 * Feature 2: Smart Task Assignment
 *   - AI analyzes task description + team members' skills
 *   - Suggests the best person to assign the task to
 *
 * Feature 3: AI Priority Suggestion
 *   - AI reads the task and suggests priority (LOW/MEDIUM/HIGH/CRITICAL)
 *   - Based on keywords, deadlines, dependencies
 *
 * Feature 4: Project Chat Bot
 *   - Chat with AI about your project
 *   - "What's the status of the payment feature?"
 *   - AI uses RAG to answer from your actual project data
 *
 * =====================================================================
 * DEPENDENCIES (add to pom.xml when ready):
 * =====================================================================
 *
 * <dependency>
 *     <groupId>org.springframework.ai</groupId>
 *     <artifactId>spring-ai-openai-spring-boot-starter</artifactId>
 * </dependency>
 *
 * application.yml:
 *   spring.ai.openai.api-key: ${OPENAI_API_KEY}
 *   spring.ai.openai.chat.model: gpt-4
 *
 * =====================================================================
 * INTERVIEW QUESTIONS:
 * =====================================================================
 *
 * Q: What is Spring AI?
 * A: A Spring framework project that provides abstractions for integrating
 *    AI models (OpenAI, Anthropic, Google, etc.) into Spring applications.
 *
 * Q: What is RAG?
 * A: Retrieval-Augmented Generation — feeding relevant context from your
 *    data to an LLM so it can answer questions about YOUR specific data.
 *
 * Q: What are embeddings?
 * A: Numerical vector representations of text. Similar texts have similar
 *    vectors, enabling semantic search (finding similar meaning, not just
 *    matching keywords).
 *
 * Q: How would you use AI in a real project?
 * A: Task summarization, smart search, content generation, chatbots,
 *    anomaly detection, recommendation systems. Always validate AI output
 *    — it can hallucinate (make up facts).
 */
public class SpringAiGuide {
    // This is a learning reference file — no executable code.
}
