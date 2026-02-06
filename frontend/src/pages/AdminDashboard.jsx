import { useEffect, useState } from 'react'
import {
  Activity,
  AlertTriangle,
  BarChart3,
  CheckCircle2,
  Clock,
  FileText,
  Loader2,
  Plus,
  ShieldCheck,
  TrendingUp,
  Users,
  XCircle,
  Zap
} from 'lucide-react'
import {
  Bar,
  BarChart,
  CartesianGrid,
  Cell,
  Legend,
  Pie,
  PieChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis
} from 'recharts'
import {
  approveEmergency,
  createCounter,
  createService,
  fetchAdminSummary,
  fetchDoctorLoad,
  fetchPendingEmergencies,
  fetchServiceStats,
  rejectEmergency
} from '../api/adminApi'
import { getTokensByStatus } from '../api/tokenApi'
import { subscribe } from '../websocket/socket'

export default function AdminDashboard() {
  const [summary, setSummary] = useState(null)
  const [emergencies, setEmergencies] = useState([])
  const [doctorLoad, setDoctorLoad] = useState([])
  const [serviceStats, setServiceStats] = useState([])
  const [emergencyToast, setEmergencyToast] = useState(null)
  const [serviceForm, setServiceForm] = useState( {name: '', 
  avgServiceTime: 0, 
  priorityAllowed: false})
  const [counterForm, setCounterForm] = useState({ name: '', status: 'OPEN' })
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState(null)
  const [peakHourData, setPeakHourData] = useState([])

  const loadData = async () => {
    try {
      const [s, e, d, stats] = await Promise.all([
        fetchAdminSummary(),
        fetchPendingEmergencies(),
        fetchDoctorLoad(),
        fetchServiceStats()
      ])
      setSummary(s)
      setEmergencies(e)
      setDoctorLoad(d)
      setServiceStats(stats)
    } catch (err) {
      setError(err.message || 'Failed to load admin data')
    }
  }

  const loadPeakHours = async () => {
    try {
      const served = await getTokensByStatus('COMPLETED')
      const waiting = await getTokensByStatus('WAITING')
      const all = [...(served || []), ...(waiting || [])]

      const today = new Date()
      const startOfDay = new Date(
        today.getFullYear(),
        today.getMonth(),
        today.getDate(),
        0,
        0,
        0,
        0
      ).getTime()
      const endOfDay = new Date(
        today.getFullYear(),
        today.getMonth(),
        today.getDate(),
        23,
        59,
        59,
        999
      ).getTime()

      const buckets = new Map()
      for (const t of all) {
        if (!t.createdAt) continue
        const ts = new Date(t.createdAt).getTime()
        if (ts < startOfDay || ts > endOfDay) continue
        const hour = new Date(ts).getHours()
        buckets.set(hour, (buckets.get(hour) || 0) + 1)
      }

      const data = Array.from({ length: 24 }, (_, hour) => ({
        hour,
        label: `${hour.toString().padStart(2, '0')}:00`,
        registrations: buckets.get(hour) || 0
      })).filter((d) => d.registrations > 0)

      setPeakHourData(data)
    } catch {
      // peak-hour chart is informational; ignore failures
      setPeakHourData([])
    }
  }

  useEffect(() => {
    loadData()
    loadPeakHours()
  }, [])

  useEffect(() => {
    const unsubscribe = subscribe('/topic/queue-updates', (event) => {
      if (event?.type === 'EMERGENCY_CREATED') {
        setEmergencyToast({
          type: event.type,
          tokenNumber: event.tokenNumber,
          serviceName: event.serviceName,
          status: event.status
        })
      }
    })

    return () => unsubscribe && unsubscribe()
  }, [])

  useEffect(() => {
    const interval = setInterval(async () => {
      try {
        const s = await fetchAdminSummary()
        setSummary(s)
      } catch {
        // swallow periodic errors to avoid noisy UI
      }
    }, 30000)

    return () => clearInterval(interval)
  }, [])

  const handleApprove = async (tokenId) => {
    setLoading(true)
    setError(null)
    try {
      await approveEmergency(tokenId)
      await loadData()
    } catch (err) {
      setError(err.message || 'Failed to approve emergency')
    } finally {
      setLoading(false)
    }
  }

  const handleReject = async (tokenId) => {
    setLoading(true)
    setError(null)
    try {
      await rejectEmergency(tokenId)
      await loadData()
    } catch (err) {
      setError(err.message || 'Failed to reject emergency')
    } finally {
      setLoading(false)
    }
  }

const handleServiceSubmit = async (e) => {
  e.preventDefault()
  setLoading(true)
  setError(null)
  try {
    // Hits POST /api/admin/services
    await createService(serviceForm) 
    setServiceForm({ name: '', avgServiceTime: 0, priorityAllowed: false })
    await loadData() // Refresh analytics and list
  } catch (err) {
    setError(err.message || 'Failed to create service')
  } finally {
    setLoading(false)
  }
}

  const handleCounterSubmit = async (e) => {
    e.preventDefault()
    setLoading(true)
    setError(null)
    try {
      await createCounter({
        name: counterForm.name,
        status: counterForm.status
      })
      setCounterForm({ name: '', status: 'OPEN' })
    } catch (err) {
      setError(err.message || 'Failed to create counter')
    } finally {
      setLoading(false)
    }
  }

  const handleDownloadDailyReport = async () => {
    try {
      setError(null)
      const served = await getTokensByStatus('COMPLETED')
      const waiting = await getTokensByStatus('WAITING')
      const all = [...served, ...waiting]

      if (!all.length) {
        setError('No tokens available for today to export')
        return
      }

      const today = new Date()
      const startOfDay = new Date(
        today.getFullYear(),
        today.getMonth(),
        today.getDate(),
        0,
        0,
        0,
        0
      ).getTime()
      const endOfDay = new Date(
        today.getFullYear(),
        today.getMonth(),
        today.getDate(),
        23,
        59,
        59,
        999
      ).getTime()

      const filtered = all.filter((t) => {
        if (!t.createdAt) return true
        const ts = new Date(t.createdAt).getTime()
        return ts >= startOfDay && ts <= endOfDay
      })

      if (!filtered.length) {
        setError('No tokens found for today to export')
        return
      }

      const headers = [
        'Token ID',
        'Token Number',
        'Status',
        'Priority',
        'Service',
        'Patient Name',
        'Doctor Name',
        'Created At'
      ]

      const escape = (value) => {
        if (value == null) return ''
        const str = String(value)
        if (str.includes('"') || str.includes(',') || str.includes('\n')) {
          return `"${str.replace(/"/g, '""')}"`
        }
        return str
      }

      const rows = filtered.map((t) => [
        escape(t.id),
        escape(t.tokenNumber),
        escape(t.status),
        escape(t.priority ? 'URGENT' : 'NORMAL'),
        escape(t.serviceName),
        escape(t.patientName),
        escape(t.doctorName),
        escape(t.createdAt)
      ])

      const csv = [headers.map(escape).join(','), ...rows.map((r) => r.join(','))].join('\r\n')

      const blob = new Blob([csv], { type: 'text/csv;charset=utf-8;' })
      const url = URL.createObjectURL(blob)
      const link = document.createElement('a')
      const todayLabel = today.toISOString().slice(0, 10)
      link.href = url
      link.setAttribute('download', `hospital-daily-report-${todayLabel}.csv`)
      document.body.appendChild(link)
      link.click()
      document.body.removeChild(link)
      URL.revokeObjectURL(url)
    } catch (err) {
      setError(err.message || 'Failed to download daily report')
    }
  }

  const bottleneckServices = serviceStats.filter(
    (s) => s.avgServiceTimeMinutes * s.waitingCount > 45
  )
  const hasBottleneck = bottleneckServices.length > 0

  return (
    <div className="min-h-[calc(100vh-56px)] bg-slate-50 py-6">
      <div className="mx-auto max-w-6xl px-4 space-y-6">
        <div className="flex items-center justify-between gap-4">
          <div className="flex items-center gap-2">
            <Activity className="w-6 h-6 text-primary-600" />
            <h1 className="text-xl font-semibold text-primary-900">Admin Dashboard</h1>
          </div>
          <button
            type="button"
            onClick={handleDownloadDailyReport}
            className="inline-flex items-center gap-2 rounded-md border border-primary-200 bg-white px-3 py-1.5 text-xs font-medium text-primary-700 shadow-sm hover:bg-primary-50"
          >
            <FileText className="w-4 h-4" />
            Download Daily Report
          </button>
        </div>

        {error && (
          <div className="mb-2 flex items-center gap-2 rounded-md border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700">
            <XCircle className="w-4 h-4" />
            <span>{error}</span>
          </div>
        )}

        {emergencyToast && (
          <div className="rounded-2xl border border-red-200 bg-red-50 px-4 py-3 flex flex-col md:flex-row md:items-center md:justify-between gap-3">
            <div className="flex items-center gap-2 text-sm text-red-800">
              <AlertTriangle className="w-5 h-5 text-red-600" />
              <div>
                <div className="font-semibold">New emergency token created</div>
                <div className="text-xs text-red-700">
                  Token {emergencyToast.tokenNumber} • {emergencyToast.serviceName} •{' '}
                  {emergencyToast.status}
                </div>
              </div>
            </div>
            <button
              type="button"
              onClick={() => {
                setEmergencyToast(null)
                // refresh emergency queue when admin chooses to view
                loadData()
              }}
              className="inline-flex items-center justify-center rounded-md bg-red-600 px-4 py-2 text-xs font-semibold text-white hover:bg-red-700"
            >
              View
            </button>
          </div>
        )}

        <section className="grid grid-cols-1 md:grid-cols-4 gap-4">
          <StatCard
            label="Served Today"
            value={summary?.tokensServedToday ?? '-'}
            accent="text-emerald-600"
          />
          <StatCard
            label="Waiting"
            value={summary?.waitingTokens ?? '-'}
            accent="text-amber-600"
          />
          <StatCard
            label="Emergency Pending"
            value={summary?.emergencyPending ?? '-'}
            accent="text-red-600"
          />
          <StatCard
            label="Emergency Approved"
            value={summary?.emergencyApproved ?? '-'}
            accent="text-sky-600"
          />
        </section>

        <section className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          <div className="lg:col-span-2 rounded-2xl border border-slate-200 bg-white p-5">
            <div className="flex items-center justify-between mb-3">
              <div className="flex items-center gap-2">
                <AlertTriangle className="w-5 h-5 text-red-500" />
                <h2 className="text-sm font-semibold text-slate-800">
                  Emergency Approval Queue
                </h2>
              </div>
            </div>
            <div className="space-y-2">
              {emergencies.length === 0 && (
                <p className="text-sm text-slate-500">
                  No pending emergency tokens at the moment.
                </p>
              )}
              {emergencies.map((t) => (
                <div
                  key={t.id}
                  className="flex items-center justify-between rounded-lg border border-slate-100 bg-slate-50 px-3 py-2 text-xs"
                >
                  <div>
                    <div className="font-semibold text-slate-800">
                      {t.tokenNumber} • {t.patientName}
                    </div>
                    <div className="text-slate-500">
                      {t.serviceName} • Priority:{' '}
                      <span className="font-medium">
                        {t.priority ? 'URGENT' : 'NORMAL'}
                      </span>
                    </div>
                  </div>
                  <div className="flex gap-2">
                    <button
                      type="button"
                      onClick={() => handleApprove(t.id)}
                      disabled={loading}
                      className="inline-flex items-center gap-1 rounded-md bg-emerald-600 px-2.5 py-1.5 text-[11px] font-medium text-white hover:bg-emerald-700 disabled:opacity-60"
                    >
                      {loading && <Loader2 className="w-3 h-3 animate-spin" />}
                      <CheckCircle2 className="w-3 h-3" />
                      Approve
                    </button>
                    <button
                      type="button"
                      onClick={() => handleReject(t.id)}
                      disabled={loading}
                      className="inline-flex items-center gap-1 rounded-md bg-red-500 px-2.5 py-1.5 text-[11px] font-medium text-white hover:bg-red-600 disabled:opacity-60"
                    >
                      {loading && <Loader2 className="w-3 h-3 animate-spin" />}
                      <XCircle className="w-3 h-3" />
                      Reject
                    </button>
                  </div>
                </div>
              ))}
            </div>
          </div>

          <div className="rounded-2xl border border-slate-200 bg-white p-5">
            <div className="flex items-center gap-2 mb-3">
              <Users className="w-5 h-5 text-primary-600" />
              <h2 className="text-sm font-semibold text-slate-800">Doctor Load</h2>
            </div>
            <div className="space-y-2 text-xs">
              {doctorLoad.length === 0 && (
                <p className="text-sm text-slate-500">
                  No doctor statistics available yet.
                </p>
              )}
              {doctorLoad.map((d) => (
                <div
                  key={d.doctorId}
                  className="rounded-lg border border-slate-100 bg-slate-50 px-3 py-2"
                >
                  <div className="font-semibold text-slate-800">{d.doctorName}</div>
                  <div className="mt-1 flex flex-wrap gap-x-3 gap-y-1 text-slate-600">
                    <span>Waiting: {d.waitingCount}</span>
                    <span>Serving: {d.servingCount}</span>
                    <span>Completed: {d.completedToday}</span>
                  </div>
                </div>
              ))}
            </div>
          </div>
        </section>

        <section className="rounded-2xl border border-slate-200 bg-white p-5">
          <div className="flex items-center justify-between mb-4">
            <div className="flex items-center gap-2">
              <Activity className="w-5 h-5 text-primary-600" />
              <h2 className="text-sm font-semibold text-slate-800">
                Department Analytics
              </h2>
            </div>
            <div className="flex items-center gap-3 text-xs text-slate-500">
              <div className="inline-flex items-center gap-1">
                <BarChart3 className="w-3.5 h-3.5 text-primary-500" />
                <span>Waiting by department</span>
              </div>
              <div className="inline-flex items-center gap-1">
                <TrendingUp className="w-3.5 h-3.5 text-emerald-500" />
                <span>Served vs waiting</span>
              </div>
              {hasBottleneck && (
                <div className="inline-flex items-center gap-1 text-red-600 font-medium">
                  <Zap className="w-3.5 h-3.5" />
                  <span>Bottleneck detected</span>
                </div>
              )}
            </div>
          </div>

          {serviceStats.length === 0 ? (
            <p className="text-sm text-slate-500">No department analytics yet.</p>
          ) : (
            <>
              {hasBottleneck && (
                <div className="mb-4 rounded-lg border border-red-300 bg-red-50 px-3 py-2 text-xs text-red-800 flex items-start gap-2">
                  <AlertTriangle className="w-4 h-4 mt-0.5" />
                  <div>
                    <div className="font-semibold">Bottleneck alert</div>
                    <div>
                      The following departments are projected to exceed{' '}
                      <span className="font-semibold">45 minutes</span> of total waiting load:
                    </div>
                    <ul className="mt-1 list-disc list-inside">
                      {bottleneckServices.map((s) => (
                        <li key={s.serviceId}>
                          {s.serviceName} —{' '}
                          {Math.round(s.avgServiceTimeMinutes * s.waitingCount)} min total load
                        </li>
                      ))}
                    </ul>
                  </div>
                </div>
              )}

              <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
                <div className="h-64">
                  <RechartsDepartmentBarChart data={serviceStats} />
                </div>
                <div className="h-64">
                  <RechartsSummaryPieChart summary={summary} />
                </div>
              </div>
            </>
          )}
        </section>

        <section className="rounded-2xl border border-slate-200 bg-white p-5">
          <div className="flex items-center justify-between mb-4">
            <div className="flex items-center gap-2">
              <Clock className="w-5 h-5 text-primary-600" />
              <h2 className="text-sm font-semibold text-slate-800">Peak Registration Hours</h2>
            </div>
            <div className="flex items-center gap-2 text-xs text-slate-500">
              <ShieldCheck className="w-3.5 h-3.5 text-emerald-500" />
              <span>Helps allocate staff during busy periods</span>
            </div>
          </div>
          {peakHourData.length === 0 ? (
            <p className="text-sm text-slate-500">
              No registration activity recorded yet for today.
            </p>
          ) : (
            <div className="h-64">
              <ResponsiveContainer width="100%" height="100%">
                <BarChart data={peakHourData} margin={{ top: 10, right: 20, left: 0, bottom: 20 }}>
                  <CartesianGrid strokeDasharray="3 3" stroke="#e2e8f0" />
                  <XAxis dataKey="label" tick={{ fontSize: 10 }} />
                  <YAxis allowDecimals={false} tick={{ fontSize: 10 }} />
                  <Tooltip
                    contentStyle={{
                      borderRadius: 8,
                      borderColor: '#e2e8f0',
                      fontSize: 12
                    }}
                  />
                  <Legend wrapperStyle={{ fontSize: 12 }} />
                  <Bar
                    dataKey="registrations"
                    name="Registrations"
                    fill="#0ea5e9"
                    radius={[4, 4, 0, 0]}
                  />
                </BarChart>
              </ResponsiveContainer>
            </div>
          )}
        </section>

        <section className="grid grid-cols-1 md:grid-cols-2 gap-6">
          <div className="rounded-2xl border border-slate-200 bg-white p-5">
  <div className="flex items-center gap-2 mb-3">
    <Plus className="w-5 h-5 text-primary-600" />
    <h2 className="text-sm font-semibold text-slate-800">
      Add New Department (ServiceType)
    </h2>
  </div>
  <form onSubmit={handleServiceSubmit} className="space-y-3">
    {/* Department Name */}
    <div className="space-y-1">
      <label className="block text-xs font-medium text-slate-700">Name</label>
      <input
        type="text"
        required
        value={serviceForm.name}
        onChange={(e) =>
          setServiceForm((prev) => ({ ...prev, name: e.target.value }))
        }
        className="w-full rounded-md border border-slate-200 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-400"
      />
    </div>

    {/* Average Service Time (New) */}
    <div className="space-y-1">
      <label className="block text-xs font-medium text-slate-700">
        Avg Service Time (mins)
      </label>
      <input
        type="number"
        min="0"
        required
        value={serviceForm.avgServiceTime}
        onChange={(e) =>
          setServiceForm((prev) => ({ ...prev, avgServiceTime: parseInt(e.target.value) || 0 }))
        }
        className="w-full rounded-md border border-slate-200 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-400"
      />
    </div>

    {/* Priority Allowed (New) */}
    <div className="flex items-center gap-2 py-1">
      <input
        id="priorityAllowed"
        type="checkbox"
        checked={serviceForm.priorityAllowed}
        onChange={(e) =>
          setServiceForm((prev) => ({ ...prev, priorityAllowed: e.target.checked }))
        }
        className="h-4 w-4 rounded border-slate-300 text-primary-600 focus:ring-primary-500"
      />
      <label htmlFor="priorityAllowed" className="text-xs font-medium text-slate-700">
        Allow Priority/Emergency Tokens
      </label>
    </div>

    <button
      type="submit"
      disabled={loading}
      className="inline-flex items-center gap-1.5 rounded-md bg-primary-600 px-3 py-2 text-xs font-medium text-white hover:bg-primary-700 disabled:opacity-60"
    >
      {loading && <Loader2 className="w-3.5 h-3.5 animate-spin" />}
      <Plus className="w-3.5 h-3.5" />
      Create Service
    </button>
  </form>
</div>

          <div className="rounded-2xl border border-slate-200 bg-white p-5">
            <div className="flex items-center gap-2 mb-3">
              <Plus className="w-5 h-5 text-primary-600" />
              <h2 className="text-sm font-semibold text-slate-800">
                Add New Counter
              </h2>
            </div>
            <form onSubmit={handleCounterSubmit} className="space-y-3">
              <div className="space-y-1">
                <label className="block text-xs font-medium text-slate-700">
                  Name
                </label>
                <input
                  type="text"
                  required
                  value={counterForm.name}
                  onChange={(e) =>
                    setCounterForm((prev) => ({ ...prev, name: e.target.value }))
                  }
                  className="w-full rounded-md border border-slate-200 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-400"
                />
              </div>
              <div className="space-y-1">
                <label className="block text-xs font-medium text-slate-700">
                  Status
                </label>
                <select
                  value={counterForm.status}
                  onChange={(e) =>
                    setCounterForm((prev) => ({ ...prev, status: e.target.value }))
                  }
                  className="w-full rounded-md border border-slate-200 px-3 py-2 text-sm bg-white focus:outline-none focus:ring-2 focus:ring-primary-400"
                >
                  <option value="OPEN">OPEN</option>
                  <option value="CLOSED">CLOSED</option>
                </select>
              </div>
              <button
                type="submit"
                disabled={loading}
                className="inline-flex items-center gap-1.5 rounded-md bg-primary-600 px-3 py-2 text-xs font-medium text-white hover:bg-primary-700 disabled:opacity-60"
              >
                {loading && <Loader2 className="w-3.5 h-3.5 animate-spin" />}
                <Plus className="w-3.5 h-3.5" />
                Create Counter
              </button>
            </form>
          </div>
        </section>
      </div>
    </div>
  )
}

function RechartsDepartmentBarChart({ data }) {
  const chartData = data.map((s) => {
    const projectedMinutes = s.avgServiceTimeMinutes * s.waitingCount
    const isBottleneck = projectedMinutes > 45
    return {
      name: s.serviceName,
      waiting: s.waitingCount,
      isBottleneck
    }
  })

  return (
    <ResponsiveContainer width="100%" height="100%">
      <BarChart data={chartData} margin={{ top: 10, right: 20, left: 0, bottom: 20 }}>
        <CartesianGrid strokeDasharray="3 3" stroke="#e2e8f0" />
        <XAxis dataKey="name" tick={{ fontSize: 11 }} />
        <YAxis tick={{ fontSize: 11 }} />
        <Tooltip
          contentStyle={{
            borderRadius: 8,
            borderColor: '#e2e8f0',
            fontSize: 12
          }}
        />
        <Legend wrapperStyle={{ fontSize: 12 }} />
        <Bar dataKey="waiting" name="Tokens waiting" radius={[4, 4, 0, 0]}>
          {chartData.map((entry, index) => (
            <Cell
              // eslint-disable-next-line react/no-array-index-key
              key={`cell-${index}`}
              fill={entry.isBottleneck ? '#ef4444' : '#0f766e'}
            />
          ))}
        </Bar>
      </BarChart>
    </ResponsiveContainer>
  )
}

function RechartsSummaryPieChart({ summary }) {
  const served = summary?.tokensServedToday ?? 0
  const waiting = summary?.waitingTokens ?? 0

  const data = [
    { name: 'Served', value: served },
    { name: 'Waiting', value: waiting }
  ]

  const hasData = served > 0 || waiting > 0

  if (!hasData) {
    return (
      <div className="h-full flex items-center justify-center text-sm text-slate-500">
        No tokens data yet for today.
      </div>
    )
  }

  return (
    <ResponsiveContainer width="100%" height="100%">
      <PieChart>
        <Tooltip
          contentStyle={{
            borderRadius: 8,
            borderColor: '#e2e8f0',
            fontSize: 12
          }}
        />
        <Legend wrapperStyle={{ fontSize: 12 }} />
        <Pie
          data={data}
          dataKey="value"
          nameKey="name"
          cx="50%"
          cy="50%"
          outerRadius={70}
          innerRadius={40}
          paddingAngle={2}
        >
          {/* Colors are applied via data index in Recharts  */}
        </Pie>
      </PieChart>
    </ResponsiveContainer>
  )
}

function StatCard({ label, value, accent }) {
  return (
    <div className="rounded-2xl border border-slate-200 bg-white px-4 py-3">
      <div className="text-xs text-slate-500">{label}</div>
      <div className={`mt-1 text-lg font-semibold ${accent}`}>{value}</div>
    </div>
  )
}

