import { useEffect, useState } from "react"
import { API } from "./api"
import { MockEvent } from "./models"
import {LoadingIcon} from './ReplView'
import { formatDistance } from 'date-fns'

const MockPage = () => {
  const [events, setEvents] = useState<MockEvent[]>()
  const [selectedEvent, setSelectedEvent] = useState<MockEvent | undefined>()
  const [isLoading, setIsLoading] = useState(false)

  useEffect(() => {
    setIsLoading(true);

    (async () => {
      const data = await API.getMockEvents()
      setEvents(data.events)
      setIsLoading(false)
    })()
  }, [])

  if (isLoading) {
    return <LoadingIcon />
  }

  return (
    <div className="flex flex-row pt-2">
      <div className="flex flex-col w-full basis-2/4 px-4">
        <h1 className="text-lg">Events</h1>
        {(events || []).map(event => (
          <div 
            className={`flex flex-row justify-between my-1 px-4 rounded-md w-full py-2 cursor-pointer text-gray-600 ${selectedEvent !== event ? 'hover:bg-gray-200' : ''} active:bg-gray-300 ${selectedEvent === event ? 'bg-gray-300' : 'bg-gray-100 '}`}
            onClick={() => setSelectedEvent(event)}
          >
            <span>{formatDistance(new Date(event.timestamp), new Date(), { addSuffix: true })}</span>
            <span>{event.path}</span>
            <span>{!!event.matched ? 'matched' : 'unmatched'}</span>
            <span>{event.statusCode}</span>
          </div>
        ))}
      </div>

      <div className="flex flex-col basis-2/4 px-4">
        <h1 className="text-lg">Response</h1>
        {!selectedEvent ? <p>No event selected</p> : null}
        {!!selectedEvent ? (
          <pre className="w-full bg-gray-200 p-4">
            {JSON.stringify(selectedEvent.response)}
          </pre>
        ) : null}
      </div>
    </div>
  )
}

export default MockPage