import SockJS from 'sockjs-client'
import { Client } from '@stomp/stompjs'

const WS_BASE_URL = import.meta.env.VITE_WS_BASE_URL || 'http://localhost:8080/ws'

let stompClient = null

export function getStompClient() {
  if (stompClient) {
    return stompClient
  }

  stompClient = new Client({
    webSocketFactory: () => new SockJS(WS_BASE_URL),
    reconnectDelay: 5000,
    heartbeatIncoming: 10000,
    heartbeatOutgoing: 10000,
    debug: () => {}
  })

  stompClient.activate()

  return stompClient
}

export function subscribe(topic, onMessage) {
  const client = getStompClient()

  let subscription = null

  const ensureSubscription = () => {
    if (client.connected && !subscription) {
      subscription = client.subscribe(topic, (message) => {
        try {
          const body = JSON.parse(message.body)
          onMessage(body)
        } catch {
          onMessage(message.body)
        }
      })
    }
  }

  if (client.connected) {
    ensureSubscription()
  } else {
    const previousOnConnect = client.onConnect
    client.onConnect = (frame) => {
      if (typeof previousOnConnect === 'function') {
        previousOnConnect(frame)
      }
      ensureSubscription()
    }
  }

  return () => {
    if (subscription) {
      subscription.unsubscribe()
    }
  }
}

