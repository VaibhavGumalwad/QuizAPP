import axios from 'axios';

const API_URL = 'http://localhost:8081/api';

const api = axios.create({
  baseURL: API_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

export const authService = {
  login: (username, password) => api.post('/auth/login', { username, password }),
  register: (username, password, email, role) => api.post('/auth/register', { username, password, email, role }),
};

export const adminService = {
  getAllUsers: () => api.get('/admin/users'),
  deleteUser: (userId) => api.delete(`/admin/user/${userId}`),
  getAllQuizzes: () => api.get('/admin/quizzes'),
  approveQuiz: (quizId) => api.put(`/admin/quiz/${quizId}/approve`),
  enhancePythonKnowledge: () => api.post('/admin/enhance-python-knowledge'),
};

export const instructorService = {
  createQuiz: (topic, numberOfQuestions, instructorId) => 
    api.post(`/instructor/quiz?instructorId=${instructorId}`, { topic, numberOfQuestions }),
  getMyQuizzes: (instructorId) => api.get(`/instructor/quizzes?instructorId=${instructorId}`),
  deleteQuiz: (quizId, instructorId) => api.delete(`/instructor/quiz/${quizId}?instructorId=${instructorId}`),
  getQuizResults: (quizId) => api.get(`/instructor/quiz/${quizId}/results`),
};

export const studentService = {
  getAllQuizzes: () => api.get('/student/quizzes'),
  getQuiz: (id) => api.get(`/student/quiz/${id}`),
  submitQuiz: (quizId, studentId, answers) => 
    api.post(`/student/quiz/${quizId}/submit?studentId=${studentId}`, { answers }),
  getMyAttempts: (studentId) => api.get(`/student/attempts?studentId=${studentId}`),
  getWeakTopics: (studentId) => api.get(`/student/weak-topics?studentId=${studentId}`),
  getAttemptAnalysis: (attemptId) => api.get(`/student/attempt/${attemptId}/analysis`),
  getDetailedFeedback: (attemptId) => api.get(`/student/attempt/${attemptId}/feedback`),
};

export const ragService = {
  askQuestion: (question, topics = [], context = {}) => 
    api.post('/rag/ask', { question, topics, context }),
  searchKnowledge: (query, limit = 5) => 
    api.get(`/rag/search?query=${encodeURIComponent(query)}&limit=${limit}`),
};

export default api;
