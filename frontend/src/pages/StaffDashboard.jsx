import { useEffect, useState } from 'react'
import {
  AlertCircle,
  CheckCircle2,
  Coffee,
  Loader2,
  PlayCircle,
  SkipForward,
  Users
} from 'lucide-react'
import { fetchServices } from '../api/serviceApi'
import { fetchCounters } from '../api/adminApi'
import { callNextToken, completeToken, skipToken } from '../api/counterApi'
import { getStompClient, subscribe } from '../websocket/socket'

export default function StaffDashboard() {
  const [services, setServices] = useState([])
  const [counters, setCounters] = useState([])
  const [selectedServiceId, setSelectedServiceId] = useState('')
  const [selectedCounterId, setSelectedCounterId] = useState('')
  const [currentToken, setCurrentToken] = useState(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState(null)
  const [lastEvent, setLastEvent] = useState(null)
  const [isConnected, setIsConnected] = useState(false)
  const [isOnBreak, setIsOnBreak] = useState(false)

  useEffect(() => {
    fetchServices().then(setServices).catch((err) => setError(err.message))
    fetchCounters().then(setCounters).catch((err) => setError(err.message))
  }, [])

  useEffect(() => {
    const client = getStompClient()
    setIsConnected(client.connected)

    const previousOnConnect = client.onConnect
    const previousOnDisconnect = client.onDisconnect

    client.onConnect = (frame) => {
      if (typeof previousOnConnect === 'function') {
        previousOnConnect(frame)
      }
      setIsConnected(true)
    }

    client.onDisconnect = (frame) => {
      if (typeof previousOnDisconnect === 'function') {
        previousOnDisconnect(frame)
      }
      setIsConnected(false)
    }

    return () => {
      client.onConnect = previousOnConnect
      client.onDisconnect = previousOnDisconnect
    }
  }, [])

  useEffect(() => {
    const unsubscribe = subscribe('/topic/counter-updates', (event) => {
      setLastEvent(event)
      if (
        currentToken &&
        event.tokenNumber === currentToken.tokenNumber &&
        event.status
      ) {
        setCurrentToken((prev) => (prev ? { ...prev, status: event.status } : prev))
      }
    })
    return () => unsubscribe && unsubscribe()
  }, [currentToken])

  const mockUpdateCounterStatus = async (counterId, status) => {
    // Since there is no real endpoint yet, this simulates:
    // PUT /api/counters/{id}/status with body { status }
    await new Promise((resolve) => setTimeout(resolve, 400))
    // In a real implementation, you would call:
    // await httpClient.put(`/api/counters/${counterId}/status`, { status })
  }

  const handleCallNext = async () => {
    if (!selectedCounterId || !selectedServiceId) {
      setError('Select both counter and department')
      return
    }
    setError(null)
    setLoading(true)
    try {
      const token = await callNextToken({
        counterId: Number(selectedCounterId),
        serviceTypeId: Number(selectedServiceId)
      })
      setCurrentToken(token)
    } catch (err) {
      setError(err.message || 'Failed to call next token')
    } finally {
      setLoading(false)
    }
  }

  const handleComplete = async () => {
    if (!currentToken) return
    setError(null)
    setLoading(true)
    try {
      await completeToken(currentToken.id)
      setCurrentToken((prev) => (prev ? { ...prev, status: 'COMPLETED' } : prev))
    } catch (err) {
      setError(err.message || 'Failed to complete token')
    } finally {
      setLoading(false)
    }
  }

  const handleSkip = async () => {
    if (!currentToken) return
    setError(null)
    setLoading(true)
    try {
      await skipToken(currentToken.id)
      setCurrentToken(null)
    } catch (err) {
      setError(err.message || 'Failed to skip token')
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="min-h-[calc(100vh-56px)] bg-slate-50 py-6">
      <div className="mx-auto max-w-6xl px-4">
        <div className="mb-6 flex items-center justify-between gap-4">
          <div className="flex items-center gap-2">
            <Users className="w-6 h-6 text-primary-600" />
            <h1 className="text-xl font-semibold text-primary-900">
              Staff / Doctor Queue Management
            </h1>
          </div>
          <div className="flex items-center gap-4">
            <div className="flex items-center gap-2 text-xs text-slate-600">
              <span
                className={`inline-block h-2 w-2 rounded-full ${
                  isConnected ? 'bg-emerald-500' : 'bg-red-500'
                }`}
              />
              <span>{isConnected ? 'Connected to queue server' : 'Disconnected'}</span>
            </div>
            <DoctorStatusToggle
              isOnBreak={isOnBreak}
              disabled={!selectedCounterId || loading}
              onToggle={async () => {
                if (!selectedCounterId) return
                const nextStatus = isOnBreak ? 'AVAILABLE' : 'ON_BREAK'
                try {
                  await mockUpdateCounterStatus(selectedCounterId, nextStatus)
                  setIsOnBreak((prev) => !prev)
                  setCounters((prev) =>
                    prev.map((c) =>
                      String(c.id) === String(selectedCounterId)
                        ? { ...c, status: nextStatus }
                        : c
                    )
                  )
                } catch (err) {
                  setError(err.message || 'Failed to update status')
                }
              }}
            />
          </div>
        </div>

        {error && (
          <div className="mb-4 flex items-center gap-2 rounded-md border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700">
            <AlertCircle className="w-4 h-4" />
            <span>{error}</span>
          </div>
        )}

        <div className="grid grid-cols-1 md:grid-cols-3 gap-4 mb-6">
          <div className="space-y-1">
            <label className="block text-xs font-medium text-slate-700">
              Assigned Counter
            </label>
            <select
              value={selectedCounterId}
              onChange={(e) => setSelectedCounterId(e.target.value)}
              className="w-full rounded-md border border-slate-200 px-3 py-2 text-sm bg-white focus:outline-none focus:ring-2 focus:ring-primary-400"
            >
              <option value="">Select counter</option>
              {counters.map((c) => (
                <option key={c.id} value={c.id}>
                  {c.name} ({c.status})
                </option>
              ))}
            </select>
          </div>
          <div className="space-y-1">
            <label className="block text-xs font-medium text-slate-700">
              Department / Service
            </label>
            <select
              value={selectedServiceId}
              onChange={(e) => setSelectedServiceId(e.target.value)}
              className="w-full rounded-md border border-slate-200 px-3 py-2 text-sm bg-white focus:outline-none focus:ring-2 focus:ring-primary-400"
            >
              <option value="">Select department</option>
              {services.map((s) => (
                <option key={s.id} value={s.id}>
                  {s.name}
                </option>
              ))}
            </select>
          </div>
          <div className="flex items-end">
            <button
              type="button"
              onClick={handleCallNext}
              disabled={loading || !selectedCounterId || !selectedServiceId}
              className="inline-flex items-center gap-2 rounded-md bg-primary-600 px-4 py-2.5 text-sm font-medium text-white shadow-sm hover:bg-primary-700 disabled:opacity-60"
            >
              {loading && (
                <Loader2 className="h-4 w-4 animate-spin" />
              )}
              <PlayCircle className="w-4 h-4" />
              Call Next
            </button>
          </div>
        </div>

        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          <div className="lg:col-span-2 rounded-2xl border border-slate-200 bg-white p-5">
            <h2 className="text-sm font-semibold text-slate-800 mb-3">
              Currently Serving
            </h2>
            {currentToken ? (
              <div className="flex flex-col md:flex-row items-center justify-between gap-4">
                <div>
                  <div className="text-xs uppercase text-slate-500">Token</div>
                  <div className="text-3xl font-semibold text-primary-800">
                    {currentToken.tokenNumber}
                  </div>
                  <div className="text-xs text-slate-500 mt-1">
                    {currentToken.serviceName}
                  </div>
                  <div className="mt-2 inline-flex items-center gap-1 rounded-full border border-emerald-100 bg-emerald-50 px-2.5 py-1 text-[11px] font-medium text-emerald-700">
                    <CheckCircle2 className="w-3 h-3" />
                    {currentToken.status}
                  </div>
                </div>
                <div className="flex gap-3">
                  <button
                    type="button"
                    onClick={handleComplete}
                    disabled={loading}
                    className="inline-flex items-center gap-1.5 rounded-md bg-emerald-600 px-3 py-2 text-xs font-medium text-white hover:bg-emerald-700 disabled:opacity-60"
                  >
                    {loading && (
                      <Loader2 className="h-3.5 w-3.5 animate-spin" />
                    )}
                    <CheckCircle2 className="w-3.5 h-3.5" />
                    Complete
                  </button>
                  <button
                    type="button"
                    onClick={handleSkip}
                    disabled={loading}
                    className="inline-flex items-center gap-1.5 rounded-md bg-amber-500 px-3 py-2 text-xs font-medium text-white hover:bg-amber-600 disabled:opacity-60"
                  >
                    {loading && (
                      <Loader2 className="h-3.5 w-3.5 animate-spin" />
                    )}
                    <SkipForward className="w-3.5 h-3.5" />
                    Skip
                  </button>
                </div>
              </div>
            ) : (
              <p className="text-sm text-slate-500">
                No patient is currently being served. Use &quot;Call Next&quot; to
                start.
              </p>
            )}
          </div>

          <div className="rounded-2xl border border-slate-200 bg-white p-5">
            <h2 className="text-sm font-semibold text-slate-800 mb-3">
              Live Counter Activity
            </h2>
            {lastEvent ? (
              <div className="space-y-1 text-xs text-slate-600">
                <div className="font-medium text-slate-800">
                  {lastEvent.type?.replace('TOKEN_', '').replace('_', ' ')}
                </div>
                <div>Token: {lastEvent.tokenNumber}</div>
                <div>Counter: {lastEvent.counterName}</div>
                <div>Service: {lastEvent.serviceName}</div>
                <div>Status: {lastEvent.status}</div>
              </div>
            ) : (
              <p className="text-sm text-slate-500">
                Waiting for live updates from this counter.
              </p>
            )}
          </div>
        </div>
      </div>
    </div>
  )
}

function DoctorStatusToggle({ isOnBreak, disabled, onToggle }) {
  return (
    <button
      type="button"
      onClick={onToggle}
      disabled={disabled}
      className={`inline-flex items-center gap-1.5 rounded-full border px-3 py-1.5 text-xs font-medium shadow-sm transition
        ${
          isOnBreak
            ? 'border-amber-300 bg-amber-50 text-amber-800 hover:bg-amber-100'
            : 'border-emerald-300 bg-emerald-50 text-emerald-800 hover:bg-emerald-100'
        }
        disabled:opacity-60 disabled:cursor-not-allowed`}
    >
      <Coffee className="w-3.5 h-3.5" />
      <span>{isOnBreak ? 'On Break' : 'Available'}</span>
    </button>
  )
}

