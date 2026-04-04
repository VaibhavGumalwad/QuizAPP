import React, { useState } from 'react';
import axios from 'axios';

const AgentInterface = () => {
  const [query, setQuery] = useState('');
  const [response, setResponse] = useState(null);
  const [loading, setLoading] = useState(false);
  const [studentData, setStudentData] = useState({
    topic_scores: {
      'Java Programming': 0.6,
      'Data Structures': 0.5,
      'Algorithms': 0.8,
      'Database': 0.4
    },
    weak_topics: ['Java Programming', 'Data Structures', 'Database']
  });

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    
    try {
      const result = await axios.post('http://localhost:8080/api/agent/process', {
        query,
        studentData,
        userId: 'demo-user'
      });
      setResponse(result.data);
    } catch (error) {
      console.error('Error:', error);
      setResponse({ message: 'Error processing request', data: null });
    } finally {
      setLoading(false);
    }
  };

  const quickActions = [
    { label: 'Analyze Performance', query: 'analyze my performance' },
    { label: 'Generate Study Plan', query: 'generate study plan' },
    { label: 'Create Assessment', query: 'create assessment on Java Programming' },
    { label: 'Get Tutoring Help', query: 'explain object-oriented programming concepts' }
  ];

  return (
    <div className="agent-interface" style={{ padding: '20px', maxWidth: '800px', margin: '0 auto' }}>
      <h2>🤖 Educational AI Agent</h2>
      
      <div style={{ marginBottom: '20px' }}>
        <h3>Quick Actions:</h3>
        <div style={{ display: 'flex', gap: '10px', flexWrap: 'wrap' }}>
          {quickActions.map((action, index) => (
            <button
              key={index}
              onClick={() => setQuery(action.query)}
              style={{
                padding: '8px 16px',
                backgroundColor: '#007bff',
                color: 'white',
                border: 'none',
                borderRadius: '4px',
                cursor: 'pointer'
              }}
            >
              {action.label}
            </button>
          ))}
        </div>
      </div>

      <form onSubmit={handleSubmit} style={{ marginBottom: '20px' }}>
        <div style={{ marginBottom: '10px' }}>
          <label>Ask the AI Agent:</label>
          <input
            type="text"
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            placeholder="e.g., analyze my performance, create study plan..."
            style={{
              width: '100%',
              padding: '10px',
              border: '1px solid #ddd',
              borderRadius: '4px',
              marginTop: '5px'
            }}
          />
        </div>
        <button
          type="submit"
          disabled={loading || !query.trim()}
          style={{
            padding: '10px 20px',
            backgroundColor: loading ? '#ccc' : '#28a745',
            color: 'white',
            border: 'none',
            borderRadius: '4px',
            cursor: loading ? 'not-allowed' : 'pointer'
          }}
        >
          {loading ? 'Processing...' : 'Send to Agent'}
        </button>
      </form>

      {response && (
        <div style={{
          border: '1px solid #ddd',
          borderRadius: '4px',
          padding: '15px',
          backgroundColor: '#f8f9fa'
        }}>
          <h3>Agent Response:</h3>
          <p><strong>Message:</strong> {response.message}</p>
          
          {response.data && (
            <div style={{ marginTop: '15px' }}>
              <h4>Detailed Analysis:</h4>
              <pre style={{
                backgroundColor: '#fff',
                padding: '10px',
                borderRadius: '4px',
                overflow: 'auto',
                fontSize: '12px'
              }}>
                {JSON.stringify(response.data, null, 2)}
              </pre>
            </div>
          )}
          
          <small style={{ color: '#666' }}>
            Response generated at: {new Date(response.timestamp).toLocaleString()}
          </small>
        </div>
      )}

      <div style={{ marginTop: '30px', padding: '15px', backgroundColor: '#e9ecef', borderRadius: '4px' }}>
        <h4>Current Student Profile:</h4>
        <pre style={{ fontSize: '12px', margin: 0 }}>
          {JSON.stringify(studentData, null, 2)}
        </pre>
      </div>
    </div>
  );
};

export default AgentInterface;