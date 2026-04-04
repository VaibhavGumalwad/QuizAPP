package com.examplatform.service;

import com.examplatform.model.KnowledgeDocument;
import com.examplatform.repository.KnowledgeDocumentRepository;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
public class KnowledgeBaseEnhancementService {
    
    private final KnowledgeDocumentRepository knowledgeRepository;
    private final RAGService ragService;
    
    public KnowledgeBaseEnhancementService(KnowledgeDocumentRepository knowledgeRepository, RAGService ragService) {
        this.knowledgeRepository = knowledgeRepository;
        this.ragService = ragService;
    }
    
    public void addPythonAdvancedConcepts() {
        List<KnowledgeDocument> advancedPythonDocs = Arrays.asList(
            createDocument("Python Programming", "Python Generators", 
                "Generators in Python are functions that return an iterator object. They use the 'yield' keyword instead of 'return'. " +
                "Key benefits: Memory efficient (lazy evaluation), can handle infinite sequences. " +
                "Example: def count_up_to(max): num = 1; while num <= max: yield num; num += 1. " +
                "Generator expressions: (x*2 for x in range(10)). " +
                "Methods: next(), send(), close(), throw(). Generators maintain state between calls.",
                "CONCEPT", "ADVANCED"),
                
            createDocument("Python Programming", "Python Decorators", 
                "Decorators are functions that modify or extend the behavior of other functions without changing their code. " +
                "Syntax: @decorator_name above function definition. " +
                "Example: def my_decorator(func): def wrapper(): print('Before'); func(); print('After'); return wrapper. " +
                "Built-in decorators: @property, @staticmethod, @classmethod. " +
                "Decorators with arguments: def decorator_with_args(arg): def decorator(func): def wrapper(): # use arg; return wrapper; return decorator. " +
                "Common uses: logging, timing, authentication, caching.",
                "CONCEPT", "ADVANCED"),
                
            createDocument("Python Programming", "Generator vs Iterator", 
                "Generators are a special type of iterator. All generators are iterators, but not all iterators are generators. " +
                "Iterator: Object implementing __iter__() and __next__() methods. " +
                "Generator: Function with yield keyword, automatically implements iterator protocol. " +
                "Memory: Generators are more memory efficient as they generate values on-demand. " +
                "State: Generators automatically maintain state between calls. " +
                "Example iterator class vs generator function comparison.",
                "EXPLANATION", "ADVANCED"),
                
            createDocument("Python Programming", "Decorator Patterns", 
                "Common decorator patterns: " +
                "1. Function decorators: @wraps from functools preserves original function metadata. " +
                "2. Class decorators: Can decorate entire classes. " +
                "3. Property decorators: @property, @setter, @deleter for getter/setter methods. " +
                "4. Multiple decorators: Stack decorators @dec1 @dec2 def func(). " +
                "5. Parameterized decorators: Decorators that accept arguments. " +
                "Real-world examples: @login_required, @cache, @retry, @timing.",
                "EXAMPLE", "ADVANCED"),
                
            createDocument("Python Programming", "Yield Keyword", 
                "The yield keyword pauses function execution and returns a value, maintaining function state. " +
                "Differences from return: yield pauses execution, return terminates function. " +
                "yield from: Delegates to another generator (Python 3.3+). " +
                "Generator methods: send() sends values to generator, throw() raises exception, close() terminates. " +
                "Use cases: Processing large datasets, infinite sequences, pipeline processing, coroutines. " +
                "Example: def fibonacci(): a, b = 0, 1; while True: yield a; a, b = b, a + b",
                "EXPLANATION", "ADVANCED"),
                
            createDocument("Python Programming", "Advanced Python Features", 
                "Context managers: with statement, __enter__ and __exit__ methods. " +
                "List comprehensions: [x*2 for x in range(10) if x%2==0]. " +
                "Lambda functions: lambda x: x*2. " +
                "Map, filter, reduce: Functional programming tools. " +
                "Closures: Inner functions accessing outer function variables. " +
                "Metaclasses: Classes that create classes. " +
                "Descriptors: Objects defining __get__, __set__, __delete__ methods.",
                "CONCEPT", "ADVANCED")
        );
        
        knowledgeRepository.saveAll(advancedPythonDocs);
        
        // Rebuild search index to include new documents
        ragService.buildSearchIndex();
        
        System.out.println("Added " + advancedPythonDocs.size() + " advanced Python documents to knowledge base");
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
}