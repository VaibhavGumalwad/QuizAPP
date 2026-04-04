import React, { useState, useEffect } from 'react';
import { studentService } from '../services/api';
import './WeakTopics.css';

const WeakTopics = () => {
  const [weakTopicsData, setWeakTopicsData] = useState(null);
  const [loading, setLoading] = useState(true);
  const user = JSON.parse(localStorage.getItem('user'));

  useEffect(() => {
    loadWeakTopics();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const loadWeakTopics = async () => {
    try {
      setLoading(true);
      const response = await studentService.getWeakTopics(user.id);
      setWeakTopicsData(response.data);
    } catch (error) {
      console.error('Error loading weak topics:', error);
    } finally {
      setLoading(false);
    }
  };

  if (loading) {
    return <div className="loading">Analyzing your performance...</div>;
  }

  if (!weakTopicsData || weakTopicsData.weakTopics.length === 0) {
    return <div className="no-data">No quiz attempts yet. Take some quizzes to see your analysis!</div>;
  }

  return (
    <div className="weak-topics-container">
      <h2>📊 Your Performance Analysis</h2>
      
      <div className="stats-summary">
        <div className="stat-card">
          <span className="stat-value">{weakTopicsData.totalAttempts}</span>
          <span className="stat-label">Total Attempts</span>
        </div>
      </div>

      <div className="topics-grid">
        {weakTopicsData.weakTopics.map((topic, index) => (
          <div key={index} className={`topic-card ${topic.isWeak ? 'weak' : 'strong'}`}>
            <div className="topic-header">
              <h3>{topic.topic}</h3>
              <span className={`score-badge ${topic.isWeak ? 'low' : 'high'}`}>
                {topic.averageScore}%
              </span>
            </div>
            <div className="topic-details">
              <p>📝 Attempts: {topic.attempts}</p>
              <p>{topic.isWeak ? '⚠️ Needs Improvement' : '✅ Good Performance'}</p>
            </div>
          </div>
        ))}
      </div>

      <div className="recommendations-section">
        <h3>🤖 AI Recommendations</h3>
        <div className="recommendations-box">
          {weakTopicsData.recommendations}
        </div>
      </div>
    </div>
  );
};

export default WeakTopics;
