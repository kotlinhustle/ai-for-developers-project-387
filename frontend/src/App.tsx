import { Route, Routes } from 'react-router'
import BookingPage from './pages/BookingPage'
import HomePage from './pages/HomePage'
import OwnerPage from './pages/OwnerPage'
import './App.css'

function App() {
  return (
    <Routes>
      <Route path="/" element={<HomePage />} />
      <Route path="/booking" element={<BookingPage />} />
      <Route path="/owner" element={<OwnerPage />} />
    </Routes>
  )
}

export default App
