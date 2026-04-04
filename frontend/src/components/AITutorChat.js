import React, { useState, useEffect, useRef } from 'react';
import { ragService, studentService } from '../services/api';
import './AITutorChat.css';

const AITutorChat = ({ studentWeakTopics = [], recentFeedback = null }) => {
  const [messages, setMessages] = useState([]);
  const [inputMessage, setInputMessage] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const messagesEndRef = useRef(null);
  const user = JSON.parse(localStorage.getItem('user'));

  useEffect(() => {
    // Welcome message with feedback integration
    let welcomeMessage = `Hi ${user?.username || 'Student'}! 👋 I'm your AI tutor powered by RAG (Retrieval-Augmented Generation). I can help you understand concepts, explain topics, and provide personalized study guidance based on our knowledge base.`;
    
    if (recentFeedback && recentFeedback.wrongAnswers && recentFeedback.wrongAnswers.length > 0) {
      welcomeMessage += `\n\n📊 I noticed you recently completed a quiz with some incorrect answers. I can help explain those concepts in detail. Just ask me about any topic you'd like to understand better!`;
    }
    
    welcomeMessage += ` What would you like to learn about?`;
    
    setMessages([{
      id: 1,
      type: 'ai',
      content: welcomeMessage,
      timestamp: new Date()
    }]);
  }, [user?.username, recentFeedback]);

  useEffect(() => {
    scrollToBottom();
  }, [messages]);

  const scrollToBottom = () => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  };

  const handleSendMessage = async () => {
    if (!inputMessage.trim() || isLoading) return;

    const userMessage = {
      id: Date.now(),
      type: 'user',
      content: inputMessage,
      timestamp: new Date()
    };

    setMessages(prev => [...prev, userMessage]);
    setInputMessage('');
    setIsLoading(true);

    try {
      const context = {
        studentId: user?.id,
        weakTopics: studentWeakTopics,
        conversationHistory: messages.slice(-5) // Last 5 messages for context
      };

      const response = await ragService.askQuestion(inputMessage, studentWeakTopics, context);
      
      const aiMessage = {
        id: Date.now() + 1,
        type: 'ai',
        content: response.data.response,
        timestamp: new Date()
      };

      setMessages(prev => [...prev, aiMessage]);
    } catch (error) {
      console.error('Error getting AI response:', error);
      const errorMessage = {
        id: Date.now() + 1,
        type: 'ai',
        content: 'Sorry, I encountered an error. Please try again or rephrase your question.',
        timestamp: new Date()
      };
      setMessages(prev => [...prev, errorMessage]);
    } finally {
      setIsLoading(false);
    }
  };

  const handleKeyPress = (e) => {
    if (e.key === 'Enter' && !e.shiftKey) {
      e.preventDefault();
      handleSendMessage();
    }
  };

  const suggestedQuestions = [
    "Explain object-oriented programming concepts",
    "How do sorting algorithms work?",
    "What are the basics of database design?",
    "Help me understand data structures",
    "Explain web development fundamentals"
  ];
  
  // Add feedback-specific suggestions if available
  const feedbackSuggestions = recentFeedback && recentFeedback.weakTopics ? 
    recentFeedback.weakTopics.map(topic => `Help me understand ${topic} better`) : [];
  
  const allSuggestions = [...feedbackSuggestions, ...suggestedQuestions].slice(0, 5);

  const handleSuggestedQuestion = (question) => {
    setInputMessage(question);
  };

  return (
    <div className="ai-tutor-chat">
      <div className="chat-header">
        <h3>🤖 AI Tutor (RAG-Powered)</h3>
        <p>Ask me anything! I'll search our knowledge base to give you accurate answers.</p>
      </div>

      <div className="chat-messages">
        {messages.map((message) => (
          <div key={message.id} className={`message ${message.type}`}>
            <div className="message-content">
              {message.content}
            </div>
            <div className="message-time">
              {message.timestamp.toLocaleTimeString()}
            </div>
          </div>
        ))}
        
        {isLoading && (
          <div className="message ai">
            <div className="message-content loading">
              <div className="typing-indicator">
                <span></span>
                <span></span>
                <span></span>
              </div>
              Searching knowledge base...
            </div>
          </div>
        )}
        
        <div ref={messagesEndRef} />
      </div>

      {messages.length === 1 && (
        <div className="suggested-questions">
          <p>Try asking:</p>
          {allSuggestions.map((question, index) => (
            <button
              key={index}
              className={`suggested-question ${index < feedbackSuggestions.length ? 'feedback-suggestion' : ''}`}
              onClick={() => handleSuggestedQuestion(question)}
            >
              {index < feedbackSuggestions.length && '🎯 '}{question}
            </button>
          ))}
        </div>
      )}

      <div className="chat-input">
        <textarea
          value={inputMessage}
          onChange={(e) => setInputMessage(e.target.value)}
          onKeyPress={handleKeyPress}
          placeholder="Ask me anything about your studies..."
          rows="2"
          disabled={isLoading}
        />
        <button 
          onClick={handleSendMessage}
          disabled={!inputMessage.trim() || isLoading}
          className="send-button"
        >
          {isLoading ? '⏳' : '📤'}
        </button>
      </div>
    </div>
  );
};

export default AITutorChat;