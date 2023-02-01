import { useEffect, useMemo, useState } from "react"
import { API } from "./api"
import { MockEvent } from "./models"
import {LoadingIcon} from './ReplView'
import { formatDistance } from 'date-fns'
import { JsonViewer } from "@textea/json-viewer"
import { response } from "msw"

const getStatusCodeColor = (statusCode: number): string => {
  if (statusCode < 400) return 'text-green-600'
  if (statusCode < 500) return 'text-orange-600'
  return 'text-red-600'
}

const MockPage = () => {
  const [events, setEvents] = useState<MockEvent[]>()
  const [selectedEvent, setSelectedEvent] = useState<MockEvent | undefined>()
  const [query, setQuery] = useState<string>('')
  const [isLoading, setIsLoading] = useState(false)

  useEffect(() => {
    setIsLoading(true);

    (async () => {
      const data = await API.getMockEvents()
      setEvents(data.events)
      setIsLoading(false)
    })()
  }, [])

  const filteredEvents = useMemo(() => {
    console.log(events, query)
    return events?.filter(event => {
      return (`${event.statusCode}`).includes(query) 
        || event.path.includes(query) 
        || JSON.stringify(event.response 
        || {}).includes(query)
        || (event.matched === true && query === 'matched')
        || (event.matched === false && query === 'unmatched')
    })
  }, [events, query])

  if (isLoading) {
    return <div className="w-full h-full flex justify-center items-center"><LoadingIcon /></div>
  }

  return (
    <div className="flex flex-row pt-2">
      <div className="flex flex-col w-full basis-2/4 px-4">
        <h1 className="text-lg">Events</h1>
        <input
          type="search"
          placeholder="Filter events by path, status code, response"
          className="rounded w-full p-4 pl-2 my-2 bg-slate-100 focus:outline-slate-900"
          value={query}
          onChange={e => setQuery(e.target.value)}
        />
        {(filteredEvents || []).map(event => (
          <div 
            className={`flex flex-col my-1 px-4 rounded-md w-full py-2 cursor-pointer text-gray-600 ${selectedEvent !== event ? 'hover:bg-gray-200' : ''} active:bg-gray-300 ${selectedEvent === event ? 'bg-gray-300' : 'bg-gray-100 '}`}
            onClick={() => setSelectedEvent(event)}
          >
            <div className="flex flex-row justify-between">
              <span>{event.path}</span>
              <span className={`${!!event.matched ? 'font-bold' : ''}`}>{!!event.matched ? 'matched' : 'unmatched'}</span>
              <span className={`${getStatusCodeColor(event.statusCode)} font-bold`}>{event.statusCode}</span>
            </div>
            <span className={"text-xs text-gray-500"}>{formatDistance(new Date(event.timestamp), new Date(), { addSuffix: true })}</span>
          </div>
        ))}
      </div>

      <div className="flex flex-col basis-2/4 px-4">
        <h1 className="text-lg">Response</h1>
        {!selectedEvent ? <p>No event selected</p> : null}
        {!!selectedEvent ? (
          <JsonViewer 
          value={selectedEvent.response || {}}
          theme="dark"
          displayDataTypes={false}
          displayObjectSize={false}
          defaultInspectDepth={6}
          rootName={false}
          style={{padding: 20, fontSize: '1.25em'}}
          />
        ) : null}
      </div>
    </div>
  )
}

export default MockPage