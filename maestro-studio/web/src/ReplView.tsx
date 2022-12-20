import { API } from './api';
import React, { ReactElement, useState } from 'react';
import AutosizingTextArea from './AutosizingTextArea';
import { ReplCommand, ReplCommandStatus } from './models';

const PlayIcon = () => {
  return (
    <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor" className="w-6 h-6">
      <path fillRule="evenodd" d="M4.5 5.653c0-1.426 1.529-2.33 2.779-1.643l11.54 6.348c1.295.712 1.295 2.573 0 3.285L7.28 19.991c-1.25.687-2.779-.217-2.779-1.643V5.653z" clipRule="evenodd" />
    </svg>
  )
}

const CheckIcon = () => {
  return (
    <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor" className="w-5 h-5 text-green-600">
      <path fillRule="evenodd" d="M19.916 4.626a.75.75 0 01.208 1.04l-9 13.5a.75.75 0 01-1.154.114l-6-6a.75.75 0 011.06-1.06l5.353 5.353 8.493-12.739a.75.75 0 011.04-.208z" clipRule="evenodd" />
    </svg>
  )
}

const ErrorIcon = () => {
  return (
    <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="currentColor" className="w-5 h-5 text-red-600">
      <path fillRule="evenodd" d="M5.47 5.47a.75.75 0 011.06 0L12 10.94l5.47-5.47a.75.75 0 111.06 1.06L13.06 12l5.47 5.47a.75.75 0 11-1.06 1.06L12 13.06l-5.47 5.47a.75.75 0 01-1.06-1.06L10.94 12 5.47 6.53a.75.75 0 010-1.06z" clipRule="evenodd" />
    </svg>
  )
}

const LoadingIcon = () => {
  return (
    <svg viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg" className="w-5 h-5 animate-spin">
      <path d="M23.9997 11.9187C24.0446 18.546 18.7085 23.9548 12.0812 23.9997C5.45398 24.0446 0.0451292 18.7085 0.000234429 12.0813C-0.0446603 5.454 5.29141 0.0451486 11.9187 0.000253852C18.5459 -0.0446409 23.9548 5.29142 23.9997 11.9187ZM3.20567 12.0596C3.23857 16.9165 7.20258 20.8272 12.0595 20.7943C16.9165 20.7614 20.8272 16.7974 20.7942 11.9404C20.7613 7.08345 16.7973 3.17279 11.9404 3.20569C7.08343 3.23859 3.17277 7.2026 3.20567 12.0596Z" fill="#2563EB" fillOpacity="0.4"/>
      <path d="M23.9997 11.9187C24.0168 14.4512 23.2323 16.9242 21.7584 18.9837C20.2846 21.0432 18.1969 22.5836 15.7942 23.3843C13.3916 24.1851 10.7972 24.2052 8.38251 23.4417C5.96776 22.6783 3.85647 21.1704 2.35085 19.134L4.92838 17.2283C6.0318 18.7208 7.57912 19.8258 9.34882 20.3854C11.1185 20.9449 13.0199 20.9301 14.7807 20.3433C16.5415 19.7564 18.0715 18.6275 19.1517 17.1182C20.2319 15.6088 20.8068 13.7964 20.7942 11.9404L23.9997 11.9187Z" fill="#2563EB"/>
    </svg>
  )
}

const CheckBox = ({type, checked, onChange}: {
  type: 'circle' | 'square'
  checked: boolean
  onChange?: (checked: boolean) => void
}) => {
  const onClick = () => {
    onChange && onChange(!checked)
  }
  if (type === 'circle') {
    if (checked) {
      return (
        <svg
          viewBox="0 0 20 20"
          fill="none"
          xmlns="http://www.w3.org/2000/svg"
          className="w-5 h-5"
          onClick={onClick}
        >
          <rect width="20" height="20" rx="10" fill="#2563EB"/>
          <path d="M7 10.25L9.6 12.85L13.5 7" stroke="white" strokeLinecap="round" strokeLinejoin="round"/>
        </svg>
      )
    } else {
      return (
        <svg
          viewBox="0 0 20 20"
          fill="none"
          xmlns="http://www.w3.org/2000/svg"
          className="w-5 h-5"
          onClick={onClick}
        >
          <rect x="0.5" y="0.5" width="19" height="19" rx="9.5" stroke="#CBD5E1"/>
        </svg>
      );
    }
  } else {
    if (checked) {
      return (
        <svg
          viewBox="0 0 20 21"
          fill="none"
          xmlns="http://www.w3.org/2000/svg"
          className="w-5 h-5 text-slate-900 hover:text-slate-600 active:text-slate-400"
          onClick={onClick}
        >
          <rect width="20" height="20" transform="translate(0 0.5)" fill="currentColor"/>
          <path d="M7 10.75L9.6 13.35L13.5 7.5" stroke="white" strokeLinecap="round" strokeLinejoin="round"/>
        </svg>
      )
    } else {
      return (
        <svg
          viewBox="0 0 20 21"
          fill="none"
          xmlns="http://www.w3.org/2000/svg"
          className="w-5 h-5 text-transparent hover:text-slate-300 active:text-slate-400"
          onClick={onClick}
        >
          <rect x="0.5" y="1" width="19" height="19" stroke="#CBD5E1" fill="currentColor"/>
        </svg>
      )
    }
  }
}

const StatusIcon = ({status}: {
  status: ReplCommandStatus
}): ReactElement | null => {
  switch (status) {
    case 'success': return <CheckIcon />
    case 'canceled': return null
    case 'error': return <ErrorIcon />
    case 'pending': return null
    case 'running': return <LoadingIcon />
  }
}

const CommandRow = ({command, selected, onClick}: {
  command: ReplCommand
  selected: boolean
  onClick: () => void
}) => {
  return (
    <div
      key={command.id}
      className="flex flex-row border-b hover:bg-slate-50 active:bg-slate-100"
      onClick={onClick}
    >
      <div
        className="flex flex-col px-2 pt-4 border-r"
      >
        <CheckBox type="circle" checked={selected} />
      </div>
      <pre className="p-4 font-mono cursor-default flex-1">{command.yaml}</pre>
      <div className="p-4">
        <StatusIcon status={command.status} />
      </div>
    </div>
  )
}

const ReplHeader = ({onSelectAll, onDeselectAll, selected}: {
  onSelectAll: () => void
  onDeselectAll: () => void
  selected: number
}) => {
  return (
    <div
      className="flex border-b items-center gap-2"
    >
      <div
        className="flex flex-col p-2 border-r"
      >
        <CheckBox
          type="square"
          checked={selected > 0}
          onChange={checked => {
            if (checked) {
              onSelectAll()
            } else {
              onDeselectAll()
            }
          }}
        />
      </div>
      <span className="data-[selectall=true]:text-slate-400 select-none" data-selectall={selected === 0}>
        {selected > 0 ? `${selected} Selected` : 'Select All'}
      </span>
    </div>
  )
}

const ReplView = () => {
  const [input, setInput] = useState("")
  const [selected, setSelected] = useState<string[]>([])
  const {error, repl} = API.repl.useRepl()

  const runCommand = () => {
    console.log(input)
    API.repl.runCommand(input)
    setInput("")
  }

  if (error) {
    return (
      <div>Error fetching repl</div>
    )
  }

  if (!repl) {
    return (
      <div>Loading...</div>
    )
  }

  return (
    <div>
      <div className="flex flex-col border">
        <ReplHeader
          onSelectAll={() => setSelected(repl.commands.map(c => c.id))}
          onDeselectAll={() => setSelected([])}
          selected={selected.length}
        />
        <div className="flex flex-col overflow-y-scroll">
          {repl.commands.map(command => (
            <CommandRow
              command={command}
              selected={selected.includes(command.id)}
              onClick={() => {
                if (selected.includes(command.id)) {
                  setSelected(prevState => prevState.filter(id => id !== command.id))
                } else {
                  setSelected(prevState => [...prevState, command.id])
                }
              }}
            />
          ))}
        </div>
        <div
          className="relative flex flex-col"
          onKeyDown={e => {
            if (e.code === 'Enter' && !e.shiftKey) {
              e.preventDefault()
              runCommand()
            }
          }}
        >
          <AutosizingTextArea
            className="resize-none p-4 overflow-scroll bg-gray-50 font-mono cursor-text outline-none border border-transparent focus:border focus:border-slate-400"
            setValue={value => setInput(value)}
            value={input}
            placeholder="Enter a command or interact with the device screenshot. Cmd-ENTER to run."
          />
          <button
            className="absolute flex items-center right-1 top-1 rounded bottom-1 px-4 disabled:text-slate-400 enabled:text-blue-600 enabled:hover:bg-slate-200 enabled:active:bg-slate-300 cursor-default"
            disabled={!input}
            onClick={() => runCommand()}
          >
            <PlayIcon />
          </button>
        </div>
      </div>
    </div>
  )
}

export default ReplView
