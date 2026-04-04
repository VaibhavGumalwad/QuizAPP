import React, { useState } from 'react';
import { adminService } from '../services/api';

const KnowledgeEnhancer = () => {
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState('');

  const enhanceKnowledge = async () => {
    setLoading(true);
    setMessage('');
    
    try {
      const response = await adminService.enhancePythonKnowledge();
      setMessage('✅ ' + response.data.message);
    } catch (error) {
      setMessage('❌ Failed to enhance knowledge base');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div style={{ 
      background: 'white', 
      padding: '20px', 
      borderRadius: '10px', 
      boxShadow: '0 2px 10px rgba(0,0,0,0.1)',
      marginBottom: '20px'
    }}>
      <h3>🧠 Knowledge Base Enhancement</h3>
      <p>Add advanced Python concepts (generators, decorators) to improve RAG responses.</p>
      
      <button 
        onClick={enhanceKnowledge}
        disabled={loading}
        style={{
          background: loading ? '#95a5a6' : 'linear-gradient(135deg, #667eea 0%, #764ba2 100%)',
          color: 'white',
          border: 'none',
          padding: '10px 20px',
          borderRadius: '8px',
          cursor: loading ? 'not-allowed' : 'pointer',
          fontWeight: '600'
        }}
      >
        {loading ? '⏳ Enhancing...' : '🚀 Enhance Python Knowledge'}
      </button>
      
      {message && (
        <div style={{ 
          marginTop: '15px', 
          padding: '10px', 
          borderRadius: '5px',
          background: message.includes('✅') ? '#d4edda' : '#f8d7da',
          color: message.includes('✅') ? '#155724' : '#721c24'
        }}>
          {message}
        </div>
      )}
    </div>
  );
};

export default KnowledgeEnhancer;