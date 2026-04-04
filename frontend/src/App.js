import React from 'react';
import { BrowserRouter as Router, Routes, Route } from 'react-router-dom';
import Login from './pages/Login';
import Register from './pages/Register';
import InstructorDashboard from './pages/InstructorDashboard';
import StudentDashboard from './pages/StudentDashboard';
import AdminDashboard from './pages/AdminDashboard';
import QuizAttempt from './pages/QuizAttempt';
import AttemptAnalysis from './components/AttemptAnalysis';
import './App.css';

function App() {
  return (
    <Router>
      <div className="App">
        <Routes>
          <Route path="/" element={<Login />} />
          <Route path="/register" element={<Register />} />
          <Route path="/instructor/dashboard" element={<InstructorDashboard />} />
          <Route path="/student/dashboard" element={<StudentDashboard />} />
          <Route path="/admin/dashboard" element={<AdminDashboard />} />
          <Route path="/student/quiz/:quizId" element={<QuizAttempt />} />
          <Route path="/student/attempt/:attemptId/analysis" element={<AttemptAnalysis />} />
        </Routes>
      </div>
    </Router>
  );
}

export default App;
