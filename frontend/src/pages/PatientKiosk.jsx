import { useEffect, useState } from 'react'
import { AlertCircle, Bell, Clock, Globe, IdCard, Loader2, Printer } from 'lucide-react'
import { fetchNonServices } from '../api/serviceApi'
import { createPatientToken, getEtaForToken } from '../api/tokenApi'
import { getStompClient, subscribe } from '../websocket/socket'

const genders = [
  { value: 'MALE', label: 'Male' },
  { value: 'FEMALE', label: 'Female' },
  { value: 'OTHER', label: 'Other' }
]

const ACTIVE_TOKEN_KEY = 'hospital_active_patient_token'

export default function PatientKiosk() {
  const [services, setServices] = useState([])
  const [form, setForm] = useState({
    name: '',
    age: '',
    phone: '',
    gender: 'MALE',
    serviceId: '',
    priority: false
  })
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState(null)
  const [tokenInfo, setTokenInfo] = useState(null)
  const [etaMinutes, setEtaMinutes] = useState(null)
  const [helpRequested, setHelpRequested] = useState(false)

  useEffect(() => {
    fetchNonServices()
      .then(setServices)
      .catch((err) => setError(err.message || 'Failed to load departments'))
  }, [])

  useEffect(() => {
    // Restore active token from this browser tab if still in progress
    const saved = sessionStorage.getItem(ACTIVE_TOKEN_KEY)
    if (!saved) return
    try {
      const parsed = JSON.parse(saved)
      if (parsed?.tokenInfo && parsed.tokenInfo.status !== 'COMPLETED') {
        setTokenInfo(parsed.tokenInfo)
        setEtaMinutes(parsed.etaMinutes ?? null)
      } else {
        sessionStorage.removeItem(ACTIVE_TOKEN_KEY)
      }
    } catch {
      sessionStorage.removeItem(ACTIVE_TOKEN_KEY)
    }
  }, [])

  useEffect(() => {
    if (!tokenInfo?.tokenNumber) return

    // Subscribe to this patient's updates
    const topic = `/topic/patient/${tokenInfo.tokenNumber}`
    const unsubscribe = subscribe(topic, (event) => {
      setTokenInfo((prev) => {
        if (!prev) return prev
        const updated = { ...prev, status: event.status }
        return updated
      })
    })

    return () => {
      unsubscribe && unsubscribe()
    }
  }, [tokenInfo?.tokenNumber])

  useEffect(() => {
    // Ensure websocket client is initialized early
    getStompClient()
  }, [])

  useEffect(() => {
    if (!tokenInfo) {
      sessionStorage.removeItem(ACTIVE_TOKEN_KEY)
      return
    }
    if (tokenInfo.status === 'COMPLETED') {
      sessionStorage.removeItem(ACTIVE_TOKEN_KEY)
      return
    }
    const payload = JSON.stringify({ tokenInfo, etaMinutes })
    sessionStorage.setItem(ACTIVE_TOKEN_KEY, payload)
  }, [tokenInfo, etaMinutes])

  const handleChange = (e) => {
    const { name, value, type, checked } = e.target
    setForm((prev) => ({
      ...prev,
      [name]: type === 'checkbox' ? checked : value
    }))
  }

  // const handleSubmit = async (e) => {
  //   e.preventDefault()
  //   setError(null)

  //   if (!form.serviceId) {
  //     setError('Please select a department')
  //     return
  //   }

  //   setSubmitting(true)
  //   try {
  //     const payload = {
  //       name: form.name.trim(),
  //       age: form.age ? Number(form.age) : null,
  //       phone: form.phone.trim(),
  //       gender: form.gender,
  //       serviceId: Number(form.serviceId),
  //       priority: Boolean(form.priority)
  //     }

  //     const token = await createPatientToken(payload)
  //     setTokenInfo(token)

  //     const eta = await getEtaForToken(token.id)
  //     setEtaMinutes(eta)
  //   } catch (err) {
  //     setError(err.message || 'Failed to register patient')
  //   } finally {
  //     setSubmitting(false)
  //   }
  // }



  const handleSubmit = async (e) => {
  e.preventDefault()
  setError(null)

  if (!form.serviceId) {
    setError('Please select a department')
    return
  }

  setSubmitting(true)

  try {
    // ✅ MATCHES CreatePatientTokenRequest DTO EXACTLY
    const payload = {
      patient: {
        name: form.name.trim(),
        age: form.age ? Number(form.age) : null,
        phone: form.phone.trim(),
        gender: form.gender
      },
      serviceTypeId: Number(form.serviceId),
      urgent: Boolean(form.priority)
    }

    const token = await createPatientToken(payload)
    setTokenInfo(token)

    const eta = await getEtaForToken(token.id)
    setEtaMinutes(eta)
  } catch (err) {
    setError(err.message || 'Failed to register patient')
  } finally {
    setSubmitting(false)
  }
}

  const handleNewToken = () => {
    setTokenInfo(null)
    setEtaMinutes(null)
    sessionStorage.removeItem(ACTIVE_TOKEN_KEY)
  }

  const handlePrint = () => {
    window.print()
  }

  const handleHelpRequest = () => {
    try {
      const client = getStompClient()
      const payload = {
        type: 'KIOSK_HELP_REQUESTED',
        kiosk: 'Main Lobby Kiosk',
        requestedAt: new Date().toISOString()
      }
      if (client && client.connected) {
        client.publish({
          destination: '/app/kiosk-help',
          body: JSON.stringify(payload)
        })
      }
      setHelpRequested(true)
      setTimeout(() => setHelpRequested(false), 8000)
    } catch {
      // soft‑fail; kiosk should still be usable
    }
  }

  const formattedDateTime = tokenInfo
    ? new Intl.DateTimeFormat('en-GB', {
        dateStyle: 'medium',
        timeStyle: 'short'
      }).format(new Date(tokenInfo.createdAt || Date.now()))
    : ''

  return (
    <>
      <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-primary-50 to-slate-100 px-4 no-print">
      <div className="w-full max-w-3xl bg-white shadow-xl rounded-2xl p-8 border border-slate-100">
        <div className="flex items-center justify-between mb-6 gap-3">
          <div className="flex items-center gap-2">
            <IdCard className="w-6 h-6 text-primary-500" />
            <div>
              <h1 className="text-2xl font-semibold text-primary-800">
                Patient Self Registration
              </h1>
              <p className="text-xs text-slate-500 flex items-center gap-1">
                <Globe className="w-3.5 h-3.5 text-primary-400" />
                Available in multiple languages soon
              </p>
            </div>
          </div>
          <button
            type="button"
            onClick={handleHelpRequest}
            className="inline-flex items-center gap-1.5 rounded-md border border-amber-300 bg-amber-50 px-3 py-1.5 text-xs font-medium text-amber-800 hover:bg-amber-100"
          >
            <Bell className="w-4 h-4" />
            I need help
          </button>
        </div>

        {error && (
          <div className="mb-4 flex items-center gap-2 rounded-md border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700">
            <AlertCircle className="w-4 h-4" />
            <span>{error}</span>
          </div>
        )}

        {helpRequested && (
          <div className="mb-4 flex items-center gap-2 rounded-md border border-amber-200 bg-amber-50 px-3 py-2 text-xs text-amber-800">
            <Bell className="w-4 h-4" />
            <span>Help requested. A staff member will be with you shortly.</span>
          </div>
        )}

        <form onSubmit={handleSubmit} className="grid grid-cols-1 md:grid-cols-2 gap-4 mb-6">
          <div className="space-y-1">
            <label className="block text-sm font-medium text-slate-700">Name</label>
            <input
              type="text"
              name="name"
              required
              value={form.name}
              onChange={handleChange}
              className="w-full rounded-md border border-slate-200 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-400"
            />
          </div>
          <div className="space-y-1">
            <label className="block text-sm font-medium text-slate-700">Age</label>
            <input
              type="number"
              name="age"
              min="0"
              value={form.age}
              onChange={handleChange}
              className="w-full rounded-md border border-slate-200 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-400"
            />
          </div>
          <div className="space-y-1">
            <label className="block text-sm font-medium text-slate-700">Phone</label>
            <input
              type="tel"
              name="phone"
              required
              value={form.phone}
              onChange={handleChange}
              className="w-full rounded-md border border-slate-200 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-400"
            />
          </div>
          <div className="space-y-1">
            <label className="block text-sm font-medium text-slate-700">Gender</label>
            <select
              name="gender"
              value={form.gender}
              onChange={handleChange}
              className="w-full rounded-md border border-slate-200 px-3 py-2 text-sm bg-white focus:outline-none focus:ring-2 focus:ring-primary-400"
            >
              {genders.map((g) => (
                <option key={g.value} value={g.value}>
                  {g.label}
                </option>
              ))}
            </select>
          </div>
          <div className="space-y-1">
            <label className="block text-sm font-medium text-slate-700">
              Department / Service
            </label>
            <select
              name="serviceId"
              required
              value={form.serviceId}
              onChange={handleChange}
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
          <div className="flex items-center gap-2 mt-6">
            <input
              id="priority"
              type="checkbox"
              name="priority"
              checked={form.priority}
              onChange={handleChange}
              className="h-4 w-4 rounded border-slate-300 text-primary-600 focus:ring-primary-500"
            />
            <label htmlFor="priority" className="text-sm font-medium text-slate-800">
              Urgent / Emergency
            </label>
          </div>
          <div className="md:col-span-2 flex justify-end mt-2">
            <button
              type="submit"
              disabled={submitting}
              className="inline-flex items-center gap-2 rounded-md bg-primary-600 px-5 py-2.5 text-sm font-medium text-white shadow-sm hover:bg-primary-700 disabled:opacity-60 disabled:cursor-not-allowed"
            >
              {submitting && (
                <Loader2 className="h-4 w-4 animate-spin" />
              )}
              Get Token
            </button>
          </div>
        </form>

        {tokenInfo && (
          <div className="rounded-xl border border-primary-100 bg-primary-50 px-4 py-3 flex flex-col md:flex-row md:items-center md:justify-between gap-3">
            <div>
              <div className="text-xs font-medium uppercase tracking-wide text-primary-600">
                Your token
              </div>
              <div className="flex items-baseline gap-3">
                <span className="text-3xl font-semibold text-primary-800">
                  {tokenInfo.tokenNumber}
                </span>
                <span className="rounded-full bg-white px-3 py-1 text-xs font-medium text-primary-700 border border-primary-100">
                  {tokenInfo.status}
                </span>
              </div>
            </div>
            <div className="flex items-center justify-between gap-4">
              <div className="flex flex-col items-start gap-1">
                <div className="flex items-center gap-2 text-sm text-primary-800">
                  <Clock className="w-4 h-4" />
                  <span>
                    ETA:{' '}
                    <span className="font-semibold">
                      {etaMinutes != null ? `${etaMinutes} min` : 'Calculating...'}
                    </span>
                  </span>
                </div>
                <div className="text-[11px] text-primary-700">
                  Issued at: <span className="font-medium">{formattedDateTime}</span>
                </div>
              </div>
              <div className="flex items-center gap-2">
                <button
                  type="button"
                  onClick={handlePrint}
                  className="inline-flex items-center gap-1.5 rounded-md border border-primary-200 bg-white px-3 py-1.5 text-xs font-medium text-primary-700 hover:bg-primary-50"
                >
                  <Printer className="w-3.5 h-3.5" />
                  Print Ticket
                </button>
                <button
                  type="button"
                  onClick={handleNewToken}
                  className="inline-flex items-center gap-1.5 rounded-md border border-primary-200 bg-white px-3 py-1.5 text-xs font-medium text-primary-700 hover:bg-primary-50"
                >
                  New Token
                </button>
              </div>
            </div>
          </div>
        )}
      </div>
      </div>

      {tokenInfo && (
        <div className="print-ticket print-only">
          <div className="ticket-header">
            <div className="ticket-hospital-name">CityCare General Hospital</div>
            <div className="ticket-subtitle">Patient Queue Token</div>
          </div>
          <div className="ticket-body">
            <div className="ticket-row ticket-token">
              <span className="ticket-label">Token</span>
              <span className="ticket-value token-number">{tokenInfo.tokenNumber}</span>
            </div>
            <div className="ticket-row">
              <span className="ticket-label">Department</span>
              <span className="ticket-value">{tokenInfo.serviceName}</span>
            </div>
            <div className="ticket-row">
              <span className="ticket-label">Issued</span>
              <span className="ticket-value">{formattedDateTime}</span>
            </div>
          </div>
          <div className="ticket-footer">
            Please be seated and watch the display board.
          </div>
        </div>
      )}
    </>
  )
}

