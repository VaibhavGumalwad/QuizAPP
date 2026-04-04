package com.examplatform.service;

import com.examplatform.model.KnowledgeDocument;
import com.examplatform.repository.KnowledgeDocumentRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.ByteBuffersDirectory;
import org.apache.lucene.store.Directory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class RAGService {
    
    private final KnowledgeDocumentRepository knowledgeRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))
            .build();
    
    @Value("${gemini.api.key}")
    private String apiKey;
    
    @Value("${gemini.api.url}")
    private String apiUrl;
    
    private Directory luceneDirectory;
    private StandardAnalyzer analyzer;
    
    public RAGService(KnowledgeDocumentRepository knowledgeRepository) {
        this.knowledgeRepository = knowledgeRepository;
        this.luceneDirectory = new ByteBuffersDirectory();
        this.analyzer = new StandardAnalyzer();
    }
    
    @PostConstruct
    public void initializeKnowledgeBase() {
        seedKnowledgeBase();
        buildSearchIndex();
    }
    
    private void seedKnowledgeBase() {
        if (knowledgeRepository.count() > 0) {
            return; // Already seeded
        }
        
        List<KnowledgeDocument> documents = Arrays.asList(
            createDocument("Java Programming", "Java Basics", 
                "Java is an object-oriented programming language. Key concepts include classes, objects, inheritance, polymorphism, and encapsulation. " +
                "Classes are blueprints for objects. Objects are instances of classes. Inheritance allows classes to inherit properties from parent classes.",
                "CONCEPT", "BEGINNER"),
                
            createDocument("Java Programming", "Object-Oriented Programming", 
                "OOP principles in Java: 1) Encapsulation - bundling data and methods together. 2) Inheritance - creating new classes based on existing ones. " +
                "3) Polymorphism - objects taking multiple forms. 4) Abstraction - hiding complex implementation details.",
                "EXPLANATION", "INTERMEDIATE"),
                
            createDocument("Data Structures", "Arrays and Lists", 
                "Arrays store elements of the same type in contiguous memory. Lists are dynamic collections that can grow/shrink. " +
                "ArrayList in Java provides dynamic array functionality. LinkedList uses nodes with pointers for efficient insertion/deletion.",
                "CONCEPT", "BEGINNER"),
                
            createDocument("Algorithms", "Sorting Algorithms", 
                "Common sorting algorithms: Bubble Sort (O(n²)), Quick Sort (O(n log n) average), Merge Sort (O(n log n) guaranteed). " +
                "Quick Sort uses divide-and-conquer with pivot selection. Merge Sort divides array and merges sorted halves.",
                "EXPLANATION", "INTERMEDIATE"),
                
            createDocument("Database", "SQL Fundamentals", 
                "SQL (Structured Query Language) manages relational databases. Basic operations: SELECT (retrieve), INSERT (add), UPDATE (modify), DELETE (remove). " +
                "JOIN operations combine data from multiple tables. WHERE clause filters results.",
                "CONCEPT", "BEGINNER"),
                
            createDocument("Web Development", "HTML and CSS", 
                "HTML structures web content using tags. CSS styles the appearance. HTML elements include headings (h1-h6), paragraphs (p), divs, spans. " +
                "CSS selectors target elements for styling. Box model includes margin, border, padding, content.",
                "CONCEPT", "BEGINNER"),
                
            createDocument("Python Programming", "Python Basics", 
                "Python is interpreted, high-level language. Key features: dynamic typing, indentation-based syntax, extensive libraries. " +
                "Data types include int, float, string, list, dict, tuple. Functions defined with 'def' keyword.",
                "CONCEPT", "BEGINNER"),
                
            createDocument("Machine Learning", "Supervised Learning", 
                "Supervised learning uses labeled training data. Common algorithms: Linear Regression (continuous output), " +
                "Logistic Regression (classification), Decision Trees, Random Forest, SVM. Training involves finding patterns in data.",
                "EXPLANATION", "ADVANCED")
        );
        
        knowledgeRepository.saveAll(documents);
        System.out.println("Knowledge base seeded with " + documents.size() + " documents");
    }
    
    private KnowledgeDocument createDocument(String topic, String title, String content, String type, String difficulty) {
        KnowledgeDocument doc = new KnowledgeDocument();
        doc.setTopic(topic);
        doc.setTitle(title);
        doc.setContent(content);
        doc.setDocumentType(type);
        doc.setDifficultyLevel(difficulty);
        return doc;
    }
    
    public void buildSearchIndex() {
        try {
            IndexWriterConfig config = new IndexWriterConfig(analyzer);
            IndexWriter writer = new IndexWriter(luceneDirectory, config);
            
            List<KnowledgeDocument> allDocs = knowledgeRepository.findAll();
            
            for (KnowledgeDocument knowledgeDoc : allDocs) {
                Document luceneDoc = new Document();
                luceneDoc.add(new StringField("id", knowledgeDoc.getId().toString(), Field.Store.YES));
                luceneDoc.add(new TextField("topic", knowledgeDoc.getTopic(), Field.Store.YES));
                luceneDoc.add(new TextField("title", knowledgeDoc.getTitle(), Field.Store.YES));
                luceneDoc.add(new TextField("content", knowledgeDoc.getContent(), Field.Store.YES));
                luceneDoc.add(new StringField("type", knowledgeDoc.getDocumentType(), Field.Store.YES));
                
                writer.addDocument(luceneDoc);
            }
            
            writer.close();
            System.out.println("Search index built with " + allDocs.size() + " documents");
            
        } catch (Exception e) {
            System.err.println("Error building search index: " + e.getMessage());
        }
    }
    
    public List<KnowledgeDocument> retrieveRelevantDocuments(String query, int maxResults) {
        List<KnowledgeDocument> results = new ArrayList<>();
        
        try {
            DirectoryReader reader = DirectoryReader.open(luceneDirectory);
            IndexSearcher searcher = new IndexSearcher(reader);
            
            QueryParser parser = new QueryParser("content", analyzer);
            Query luceneQuery = parser.parse(query);
            
            TopDocs topDocs = searcher.search(luceneQuery, maxResults);
            
            for (ScoreDoc scoreDoc : topDocs.scoreDocs) {
                Document doc = searcher.doc(scoreDoc.doc);
                Long docId = Long.parseLong(doc.get("id"));
                
                knowledgeRepository.findById(docId).ifPresent(results::add);
            }
            
            reader.close();
            
        } catch (Exception e) {
            System.err.println("Error searching documents: " + e.getMessage());
            // Fallback to simple database search
            return knowledgeRepository.findByTopicContainingIgnoreCase(query);
        }
        
        return results;
    }
    
    public String generateRAGResponse(String userQuery, List<String> weakTopics, Map<String, Object> context) {
        // 1. Retrieve relevant documents
        String searchQuery = String.join(" ", weakTopics) + " " + userQuery;
        List<KnowledgeDocument> relevantDocs = retrieveRelevantDocuments(searchQuery, 5);
        
        // 2. Build context from retrieved documents
        String retrievedContext = relevantDocs.stream()
                .map(doc -> String.format("Topic: %s\nTitle: %s\nContent: %s", 
                        doc.getTopic(), doc.getTitle(), doc.getContent()))
                .collect(Collectors.joining("\n\n---\n\n"));
        
        // 3. Generate response using retrieved context
        return generateAugmentedResponse(userQuery, retrievedContext, context);
    }
    
    public String generateTopicExplanation(String topic, String specificQuestion) {
        // Retrieve documents specific to the topic
        List<KnowledgeDocument> topicDocs = retrieveRelevantDocuments(topic + " " + specificQuestion, 3);
        
        String context = topicDocs.stream()
                .map(doc -> doc.getContent())
                .collect(Collectors.joining("\n\n"));
        
        if (apiKey == null || apiKey.equals("YOUR_GEMINI_API_KEY") || apiKey.trim().isEmpty()) {
            return "Based on our knowledge base: " + 
                   (context.length() > 300 ? context.substring(0, 300) + "..." : context);
        }
        
        try {
            String prompt = String.format(
                "You are an educational AI tutor. Explain the following topic clearly and concisely.\n\n" +
                "TOPIC: %s\n" +
                "SPECIFIC QUESTION: %s\n\n" +
                "KNOWLEDGE BASE CONTEXT:\n%s\n\n" +
                "Provide a clear, educational explanation that helps the student understand this topic. " +
                "Include key concepts, examples, and practical applications where relevant.",
                topic, specificQuestion, context
            );
            
            String requestBody = objectMapper.writeValueAsString(Map.of(
                "contents", List.of(Map.of(
                    "parts", List.of(Map.of("text", prompt))
                ))
            ));
            
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl + "?key=" + apiKey))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(30))
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();
            
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                JsonNode root = objectMapper.readTree(response.body());
                return root.path("candidates").get(0)
                        .path("content").path("parts").get(0)
                        .path("text").asText();
            }
            
        } catch (Exception e) {
            System.err.println("Error generating topic explanation: " + e.getMessage());
        }
        
        return "Based on our knowledge base: " + 
               (context.length() > 300 ? context.substring(0, 300) + "..." : context);
    }
    
    private String generateAugmentedResponse(String query, String retrievedContext, Map<String, Object> context) {
        if (apiKey == null || apiKey.equals("YOUR_GEMINI_API_KEY") || apiKey.trim().isEmpty()) {
            return "Configure Gemini API for RAG-powered responses. Using retrieved knowledge: " + 
                   (retrievedContext.length() > 200 ? retrievedContext.substring(0, 200) + "..." : retrievedContext);
        }
        
        try {
            String prompt = String.format(
                "You are an AI tutor with access to educational knowledge base. " +
                "Use the following retrieved knowledge to answer the student's question accurately and helpfully.\n\n" +
                "RETRIEVED KNOWLEDGE:\n%s\n\n" +
                "STUDENT CONTEXT:\n%s\n\n" +
                "STUDENT QUESTION: %s\n\n" +
                "Provide a comprehensive, educational response using the retrieved knowledge. " +
                "Include specific examples and actionable study tips.",
                retrievedContext,
                context.toString(),
                query
            );
            
            String requestBody = objectMapper.writeValueAsString(Map.of(
                "contents", List.of(Map.of(
                    "parts", List.of(Map.of("text", prompt))
                ))
            ));
            
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiUrl + "?key=" + apiKey))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(30))
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();
            
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                JsonNode root = objectMapper.readTree(response.body());
                return root.path("candidates").get(0)
                        .path("content").path("parts").get(0)
                        .path("text").asText();
            }
            
        } catch (Exception e) {
            System.err.println("Error generating RAG response: " + e.getMessage());
        }
        
        return "Based on the knowledge base: " + 
               (retrievedContext.length() > 300 ? retrievedContext.substring(0, 300) + "..." : retrievedContext);
    }
}