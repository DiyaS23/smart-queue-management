import { useMemo, useState } from 'react'
import { AlertCircle, History as HistoryIcon, Loader2, Search } from 'lucide-react'
import {
  fetchPatientHistory,
  fetchPatientHistoryByDoctor,
  fetchPatientHistoryByService
} from '../api/patientApi'

function formatDateTime(value) {
  if (!value) return '—'
  const d = new Date(value)
  if (Number.isNaN(d.getTime())) return String(value)
  return d.toLocaleString()
}

export default function PatientHistory() {
  const [phone, setPhone] = useState('')
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState(null)
  const [rows, setRows] = useState([])
  const [filterMode, setFilterMode] = useState('ALL') // ALL | SERVICE | DOCTOR
  const [filterValue, setFilterValue] = useState('')

  const normalizedPhone = useMemo(() => phone.trim(), [phone])

  const handleSearch = async (e) => {
    e.preventDefault()
    setError(null)

    if (!normalizedPhone) {
      setError('Enter a phone number to search.')
      return
    }

    setLoading(true)
    try {
      let data
      if (filterMode === 'SERVICE') {
        if (!filterValue) throw new Error('Enter a Department (Service) ID.')
        data = await fetchPatientHistoryByService(normalizedPhone, Number(filterValue))
      } else if (filterMode === 'DOCTOR') {
        if (!filterValue) throw new Error('Enter a Doctor (Counter) ID.')
        data = await fetchPatientHistoryByDoctor(normalizedPhone, Number(filterValue))
      } else {
        data = await fetchPatientHistory(normalizedPhone)
      }
      setRows(data)
    } catch (err) {
      setError(err.message || 'Failed to load patient history')
      setRows([])
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="min-h-[calc(100vh-56px)] bg-slate-50 py-6">
      <div className="mx-auto max-w-6xl px-4">
        <div className="mb-6 flex items-center gap-2">
          <HistoryIcon className="w-6 h-6 text-primary-600" />
          <h1 className="text-xl font-semibold text-primary-900">Patient History</h1>
        </div>

        <form
          onSubmit={handleSearch}
          className="rounded-2xl border border-slate-200 bg-white p-5"
        >
          {error && (
            <div className="mb-4 flex items-center gap-2 rounded-md border border-red-200 bg-red-50 px-3 py-2 text-sm text-red-700">
              <AlertCircle className="w-4 h-4" />
              <span>{error}</span>
            </div>
          )}

          <div className="grid grid-cols-1 md:grid-cols-12 gap-3 items-end">
            <div className="md:col-span-5">
              <label className="block text-xs font-medium text-slate-700 mb-1">
                Phone number
              </label>
              <input
                value={phone}
                onChange={(e) => setPhone(e.target.value)}
                className="w-full rounded-md border border-slate-200 px-3 py-2 text-sm focus:outline-none focus:ring-2 focus:ring-primary-400"
                placeholder="e.g. 9876543210"
              />
            </div>

            <div className="md:col-span-3">
              <label className="block text-xs font-medium text-slate-700 mb-1">
                Filter
              </label>
              <select
                value={filterMode}
                onChange={(e) => {
                  setFilterMode(e.target.value)
                  setFilterValue('')
                }}
                className="w-full rounded-md border border-slate-200 px-3 py-2 text-sm bg-white focus:outline-none focus:ring-2 focus:ring-primary-400"
              >
                <option value="ALL">All visits</option>
                <option value="SERVICE">By Department (Service ID)</option>
                <option value="DOCTOR">By Doctor (Counter ID)</option>
              </select>
            </div>

            <div className="md:col-span-2">
              <label className="block text-xs font-medium text-slate-700 mb-1">
                {filterMode === 'SERVICE'
                  ? 'Service ID'
                  : filterMode === 'DOCTOR'
                    ? 'Doctor ID'
                    : '—'}
              </label>
              <input
                value={filterValue}
                onChange={(e) => setFilterValue(e.target.value)}
                disabled={filterMode === 'ALL'}
                className="w-full rounded-md border border-slate-200 px-3 py-2 text-sm disabled:bg-slate-50 focus:outline-none focus:ring-2 focus:ring-primary-400"
                placeholder={filterMode === 'ALL' ? '' : 'Enter ID'}
              />
            </div>

            <div className="md:col-span-2">
              <button
                type="submit"
                disabled={loading}
                className="w-full inline-flex items-center justify-center gap-2 rounded-md bg-primary-600 px-4 py-2.5 text-sm font-medium text-white hover:bg-primary-700 disabled:opacity-60"
              >
                {loading ? <Loader2 className="w-4 h-4 animate-spin" /> : <Search className="w-4 h-4" />}
                Search
              </button>
            </div>
          </div>
        </form>

        <div className="mt-6 rounded-2xl border border-slate-200 bg-white overflow-hidden">
          <div className="px-5 py-3 border-b border-slate-100 flex items-center justify-between">
            <div className="text-sm font-semibold text-slate-800">Visits</div>
            <div className="text-xs text-slate-500">{rows.length} result(s)</div>
          </div>
          <div className="overflow-auto">
            <table className="min-w-full text-sm">
              <thead className="bg-slate-50 text-slate-600">
                <tr>
                  <th className="text-left px-5 py-3 font-medium">Token</th>
                  <th className="text-left px-5 py-3 font-medium">Department</th>
                  <th className="text-left px-5 py-3 font-medium">Doctor</th>
                  <th className="text-left px-5 py-3 font-medium">Status</th>
                  <th className="text-left px-5 py-3 font-medium">Created</th>
                  <th className="text-left px-5 py-3 font-medium">Called</th>
                  <th className="text-left px-5 py-3 font-medium">Completed</th>
                </tr>
              </thead>
              <tbody>
                {rows.length === 0 ? (
                  <tr>
                    <td colSpan={7} className="px-5 py-8 text-center text-slate-500">
                      No history to display.
                    </td>
                  </tr>
                ) : (
                  rows.map((r) => (
                    <tr key={r.tokenId} className="border-t border-slate-100">
                      <td className="px-5 py-3 font-medium text-primary-800">{r.tokenNumber}</td>
                      <td className="px-5 py-3">{r.serviceName}</td>
                      <td className="px-5 py-3">{r.doctorName || '—'}</td>
                      <td className="px-5 py-3">{r.status}</td>
                      <td className="px-5 py-3">{formatDateTime(r.createdAt)}</td>
                      <td className="px-5 py-3">{formatDateTime(r.calledAt)}</td>
                      <td className="px-5 py-3">{formatDateTime(r.completedAt)}</td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </div>
  )
}

