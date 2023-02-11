import { UIElement } from './models';
import { motion } from 'framer-motion';
import React, { useLayoutEffect, useMemo, useState } from 'react';
import { CommandExample, getCommandExamples } from './commandExample';
import { useHotkeys } from 'react-hotkeys-hook';
import copy from 'copy-to-clipboard';

const KeyPill = ({text}: {
  text: string
}) => {
  return (
    <span className="whitespace-nowrap flex items-center px-2 h-5 bg-slate-200 text-slate-400 font-semibold text-xs rounded">
      {text}
    </span>
  )
}

const Shortcut = ({text, action}: {
  text: string | string[]
  action: string
}) => {
  const textArray: string[] = typeof text === 'string' ? [text] : text
  return (
    <div className="flex gap-1.5">
      <div className="flex gap-1">
        {textArray.map(key => (
          <KeyPill text={key} />
        ))}
      </div>
      <span className="text-slate-400 text-sm whitespace-nowrap">
        {action}
      </span>
    </div>
  )
}

const SearchBar = () => {
  return (
    <div className="flex items-center bg-slate-50 border-b border-b-slate-200">
      <input
        className="outline-none py-5 pl-5 pr-5 flex-1 bg-transparent min-w-[175px]"
        placeholder="Search commands"
        autoFocus={true}
      />
      <div className="flex gap-x-3 gap-y-2 pr-5 flex-wrap py-5 justify-end">
        <Shortcut text={['\u25B2', '\u25BC']} action={"Select"} />
        <Shortcut text={'\u2318 C'} action={"Copy"} />
        <Shortcut text={'\u2318 D'} action={"Docs"} />
        <Shortcut text={'\u2318 \u23CE'} action={"Edit"} />
        <Shortcut text={'\u23CE'} action={"Run"} />
        <Shortcut text={'ESC'} action={"Close"} />
      </div>
    </div>
  )
}

const ActionRow = ({example, focused}: {
  example: CommandExample
  focused: boolean
}) => {
  const headerBg = focused ? 'bg-blue-800' : 'bg-slate-50'
  const headerTextColor = focused ? 'text-white' : 'text-slate-900'
  const borderColor = focused ? 'border-blue-800' : 'border-slate-300'
  const contentBg = focused ? 'bg-blue-50' : 'bg-white'
  return (
    <div
      className="action-row flex flex-col py-2.5"
      aria-selected={focused}
    >
      <div className={`flex flex-col rounded border ${contentBg} ${borderColor}`}>
      <span className={`font-semibold px-4 py-3 border-b font-mono text-sm ${headerBg} ${headerTextColor} ${borderColor}`}>
        {example.title}
      </span>
        <pre className={`text-slate-900 p-4 overflow-x-scroll`}>
        {example.content}
      </pre>
      </div>
    </div>
  )
}

const ActionList = ({examples, focused}: {
  examples: CommandExample[]
  focused: CommandExample | null
}) => {
  return (
    <div className="flex-1 flex flex-col px-5 py-2.5 overflow-y-scroll">
      {examples.map(example => (
        <ActionRow
          example={example}
          focused={example === focused}
        />
      ))}
    </div>
  )
}

// https://github.com/pacocoursey/cmdk/blob/9a808d096d6a9a24c0222bf8b0784fbbc6f184d4/cmdk/src/index.tsx#LL875-L883C2
function useLazyRef<T>(fn: () => T) {
  const ref = React.useRef<T>()

  if (ref.current === undefined) {
    ref.current = fn()
  }

  return ref as React.MutableRefObject<T>
}

// https://github.com/pacocoursey/cmdk/blob/9a808d096d6a9a24c0222bf8b0784fbbc6f184d4/cmdk/src/index.tsx#L937
const useScheduleLayoutEffect = () => {
  const [s, ss] = React.useState<object>()
  const fns = useLazyRef(() => new Map<string | number, () => void>())

  useLayoutEffect(() => {
    fns.current.forEach((f) => f())
    fns.current = new Map()
  }, [s, fns])

  return (id: string | number, cb: () => void) => {
    fns.current.set(id, cb)
    ss({})
  }
}

const useFocused = (examples: CommandExample[]): {
  focused: CommandExample | null,
  moveFocus: (by: number) => void,
} => {
  const [userSelectedTitle, setUserSelectedTitle] = useState<string | null>(null)
  const userSelectedIndex = examples.findIndex(e => e.title === userSelectedTitle)
  const focusedIndex = userSelectedIndex === -1 ? 0 : userSelectedIndex
  const focused = examples.length === 0 ? null : examples[focusedIndex]
  const schedule = useScheduleLayoutEffect()

  const scrollSelectedIntoView = () => {
    const selected = document.querySelector('.action-row[aria-selected=true]')
    selected?.scrollIntoView({block: 'nearest'})
  }

  const moveFocus = (by: number) => {
    const newFocusedIndex = Math.max(0, Math.min(examples.length - 1, focusedIndex + by))
    schedule(0, scrollSelectedIntoView)
    if (focusedIndex === newFocusedIndex) return
    const newSelected = examples[newFocusedIndex]
    setUserSelectedTitle(newSelected.title)
  }

  return { focused, moveFocus }
}

export const ActionModal = ({ uiElement, onEdit, onRun, onClose }: {
  uiElement: UIElement
  onEdit: (example: CommandExample) => void
  onRun: (example: CommandExample) => void
  onClose: () => void
}) => {
  const examples = useMemo(() => getCommandExamples(uiElement), [uiElement])
  const { focused, moveFocus } = useFocused(examples);
  useHotkeys('up, down', e => {
    if (e.code === 'ArrowUp') {
      moveFocus(-1)
    } else if (e.code === 'ArrowDown') {
      moveFocus(1)
    }
  }, {preventDefault: true, enableOnFormTags: true})
  useHotkeys('meta+c', () => {
    const toCopy = focused?.content
    if (!toCopy) return
    copy(toCopy)
  }, {preventDefault: true, enableOnFormTags: true})
  useHotkeys('meta+d', () => {
    const documentation = focused?.documentation
    if (!documentation) return
    window.open(documentation, '_blank', 'noreferrer');
  }, {preventDefault: true, enableOnFormTags: true})
  useHotkeys('meta+enter', () => {
    focused && onEdit(focused)
  }, {preventDefault: true, enableOnFormTags: true})
  useHotkeys('enter', () => {
    focused && onRun(focused)
  }, {preventDefault: true, enableOnFormTags: true})
  useHotkeys('escape', onClose, {preventDefault: true, enableOnFormTags: true})
  return (
    <div className="absolute inset-0 bg-slate-900/60 z-50 p-10">
      <motion.div
        className="flex flex-col h-full min-w-[70%] min-h-[70%] max-w-[1000px] bg-white rounded-lg overflow-hidden"
        initial={{ scale: .97, opacity: 0 }}
        animate={{ scale: 1, opacity: 1 }}
        transition={{ ease: 'easeOut', duration: .1 }}
        onClick={e => e.stopPropagation()}
      >
        <SearchBar />
        <ActionList examples={examples} focused={focused}/>
      </motion.div>
    </div>
  )
}
