import { useEffect, useState } from 'react'
import { Activity, MonitorPlay, TrendingUp } from 'lucide-react'
import { getStompClient, subscribe } from '../websocket/socket'
import { getTokensByStatus } from '../api/tokenApi'

export default function DisplayBoard() {
  const [nowServing, setNowServing] = useState(null)
  const [recentTokens, setRecentTokens] = useState([])
  const [isConnected, setIsConnected] = useState(false)
  const [comingUpNext, setComingUpNext] = useState([])

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
    const refreshUpcoming = async () => {
      try {
        const waiting = await getTokensByStatus('WAITING')
        if (!Array.isArray(waiting)) {
          setComingUpNext([])
          return
        }
        const sorted = [...waiting].sort((a, b) => {
          const aTime = a.createdAt ? new Date(a.createdAt).getTime() : 0
          const bTime = b.createdAt ? new Date(b.createdAt).getTime() : 0
          return aTime - bTime || String(a.tokenNumber).localeCompare(String(b.tokenNumber))
        })
        setComingUpNext(sorted.slice(0, 3))
      } catch {
        // silently ignore display-only failures
      }
    }

    refreshUpcoming()

    const unsubscribe = subscribe('/topic/queue-updates', (event) => {
      if (event.type === 'TOKEN_CALLED') {
        setNowServing(event)
        setRecentTokens((prev) => {
          const updated = [event, ...prev]
          return updated.slice(0, 5)
        })
        refreshUpcoming()
      }
    })

    return () => unsubscribe && unsubscribe()
  }, [])

  return (
    <div className="min-h-screen bg-gradient-to-br from-primary-900 via-primary-800 to-slate-900 text-white flex flex-col items-center justify-center px-8">
      <div className="w-full max-w-6xl">
        <header className="mb-10 flex items-center justify-between">
          <div className="flex items-center gap-3">
            <MonitorPlay className="w-10 h-10 text-accent-300" />
            <div>
              <h1 className="text-3xl font-semibold tracking-tight">
                Hospital Live Queue Board
              </h1>
              <p className="text-sm text-slate-200">
                Tokens update instantly as patients are called
              </p>
            </div>
          </div>
          <div className="flex items-center gap-3 text-xs text-slate-100">
            <div className="flex items-center gap-1.5">
              <span
                className={`inline-block h-2 w-2 rounded-full ${
                  isConnected ? 'bg-emerald-400' : 'bg-red-400'
                }`}
              />
              <span>{isConnected ? 'Connected' : 'Disconnected'}</span>
            </div>
            <Activity className="w-4 h-4 text-emerald-300" />
          </div>
        </header>

        <main className="grid grid-cols-1 lg:grid-cols-3 gap-8 items-stretch">
          <section className="lg:col-span-2 bg-white/10 backdrop-blur-md rounded-2xl border border-white/10 p-8 flex flex-col items-center justify-center">
            <h2 className="text-xl font-medium text-accent-100 mb-4">Now Serving</h2>
            {nowServing ? (
              <>
                <div className="text-7xl font-bold tracking-widest mb-4">
                  {nowServing.tokenNumber}
                </div>
                <div className="flex flex-col items-center gap-1 text-lg">
                  <span className="font-medium">
                    Counter: <span className="font-semibold">{nowServing.counterName}</span>
                  </span>
                  <span className="text-accent-100">
                    Department:{' '}
                    <span className="font-semibold">{nowServing.serviceName}</span>
                  </span>
                </div>
              </>
            ) : (
              <p className="text-lg text-slate-200">Waiting for the next patient...</p>
            )}

            <div className="w-full mt-8 pt-6 border-t border-white/10">
              <div className="flex items-center justify-between mb-3">
                <div className="flex items-center gap-2">
                  <TrendingUp className="w-5 h-5 text-emerald-300" />
                  <h3 className="text-base font-medium text-accent-100">
                    Coming Up Next
                  </h3>
                </div>
                <span className="text-xs text-slate-200">
                  Based on live waiting queue
                </span>
              </div>
              {comingUpNext.length === 0 ? (
                <p className="text-sm text-slate-200">
                  No additional patients are currently waiting.
                </p>
              ) : (
                <div className="space-y-2">
                  {comingUpNext.map((token, index) => {
                    const isPreCall = index === 0
                    return (
                      <div
                        key={token.id}
                        className={`flex items-center justify-between rounded-lg px-3 py-2 text-sm ${
                          isPreCall ? 'bg-emerald-900/40 border border-emerald-400/60' : 'bg-black/20'
                        }`}
                      >
                        <div>
                          <div className="font-semibold">{token.tokenNumber}</div>
                          <div className="text-xs text-slate-200">
                            {token.serviceName}
                            {token.priority && ' • Emergency'}
                          </div>
                        </div>
                        <div className="flex flex-col items-end gap-1">
                          <span className="text-xs uppercase tracking-wide text-amber-300">
                            Waiting
                          </span>
                          {isPreCall && (
                            <span className="inline-flex items-center rounded-full bg-emerald-400 px-2 py-0.5 text-[10px] font-semibold uppercase tracking-wide text-emerald-950 shadow-sm">
                              Please Proceed to Lobby
                            </span>
                          )}
                        </div>
                      </div>
                    )
                  })}
                </div>
              )}
            </div>
          </section>

          <section className="bg-white/10 backdrop-blur-md rounded-2xl border border-white/10 p-6">
            <h2 className="text-lg font-medium text-accent-100 mb-4">Recently Called</h2>
            <div className="space-y-2">
              {recentTokens.length === 0 && (
                <p className="text-sm text-slate-200">No tokens called yet.</p>
              )}
              {recentTokens.map((token) => (
                <div
                  key={`${token.tokenNumber}-${token.counterName}`}
                  className="flex items-center justify-between rounded-lg bg-black/20 px-3 py-2 text-sm"
                >
                  <div>
                    <div className="font-semibold">{token.tokenNumber}</div>
                    <div className="text-xs text-slate-200">
                      {token.serviceName} • {token.counterName}
                    </div>
                  </div>
                  <span className="text-xs uppercase tracking-wide text-emerald-300">
                    {token.status}
                  </span>
                </div>
              ))}
            </div>
          </section>
        </main>
      </div>
    </div>
  )
}

