import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'
import PatientKiosk from './pages/PatientKiosk'
import DisplayBoard from './pages/DisplayBoard'
import Login from './pages/Login'
import PatientHistory from './pages/PatientHistory'
import StaffDashboard from './pages/StaffDashboard'
import AdminDashboard from './pages/AdminDashboard'
import { AuthProvider } from './context/AuthContext'
import ProtectedRoute from './components/ProtectedRoute'
import Navbar from './components/Navbar'

function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <div className="min-h-screen flex flex-col">
          <Navbar />
          <main className="flex-1">
            <Routes>
              <Route path="/" element={<Navigate to="/kiosk" replace />} />
              <Route path="/kiosk" element={<PatientKiosk />} />
              <Route path="/display" element={<DisplayBoard />} />
              <Route path="/history" element={<PatientHistory />} />
              <Route path="/login" element={<Login />} />

              <Route
                element={
                  <ProtectedRoute allowedRoles={['ROLE_STAFF', 'ROLE_ADMIN']} />
                }
              >
                <Route path="/staff" element={<StaffDashboard />} />
              </Route>

              <Route
                element={<ProtectedRoute allowedRoles={['ROLE_ADMIN']} />}
              >
                <Route path="/admin" element={<AdminDashboard />} />
              </Route>

              <Route path="*" element={<Navigate to="/kiosk" replace />} />
            </Routes>
          </main>
        </div>
      </AuthProvider>
    </BrowserRouter>
  )
}

export default App
