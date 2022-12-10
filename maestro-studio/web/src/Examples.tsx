import React, { useState } from 'react';
import { DeviceScreen, UIElement } from './models';
import { CopyToClipboard } from 'react-copy-to-clipboard';

const CodeSnippet = ({children}: {
  children: string
}) => {
  const [hovered, setHovered] = useState(false)
  const [copied, setCopied] = useState(false)
  return (
    <CopyToClipboard text={children} onCopy={() => setCopied(true)}>
      <div
        className="relative bg-slate-100 rounded border border-transparent hover:border-slate-500 active:bg-slate-200 cursor-context-menu"
        onMouseOver={() => setHovered(true)}
        onMouseLeave={() => {
          setHovered(false)
          setCopied(false)
        }}
      >
        <pre className="overflow-x-scroll p-5">
          {children}
        </pre>
        {hovered ? (
          <div className="absolute top-0 right-0 flex">
            <span className="px-2 py-1 font-sans text-slate-400">{copied ? 'copied' : 'click to copy'}</span>
          </div>
        ) : null}
      </div>
    </CopyToClipboard>
  );
}

const Section = ({ deviceScreen, element, title, documentationUrl, codeSnippets }: {
  deviceScreen: DeviceScreen
  element: UIElement
  title: string
  documentationUrl: string
  codeSnippets: string[]
}) => {
  const toPercent = (n: number, total: number) => `${Math.round((100 * n / total))}%`
  const codeSnippetComponents = codeSnippets.map(codeSnippet => {
    if (codeSnippet.includes('[id]') && !element.resourceId) return null
    if (codeSnippet.includes('[text]') && !element.text) return null
    if (codeSnippet.includes('[bounds]') && !element.bounds) return null
    const id = element.resourceId || ''
    const text = element.text || ''
    const bounds = element.bounds || { x: 0, y: 0, width: 0, height: 0 }
    const cx = toPercent(bounds.x + bounds.width / 2, deviceScreen.width)
    const cy = toPercent(bounds.y + bounds.height / 2, deviceScreen.height)
    const point = `[${cx},${cy}]`
    return (
      <CodeSnippet>
        {
          codeSnippet
            .replace('[id]', id)
            .replace('[text]', text)
            .replace('[point]', point)
        }
      </CodeSnippet>
    )
  }).filter(c => c)
  if (codeSnippets.length === 0) return null
  return (
    <div className="flex flex-col gap-3">
      <div className="flex gap-2 justify-between">
        <span className="text-slate-500 whitespace-nowrap">{title}</span>
        <a
          className="text-blue-400 underline underline-offset-2 whitespace-nowrap"
          href={documentationUrl}
          target="_blank"
          rel="noopener noreferrer"
        >View Documentation</a>
      </div>
      {codeSnippetComponents}
    </div>
  )
}

const Examples = ({ deviceScreen, element }: {
  deviceScreen: DeviceScreen
  element: UIElement
}) => {
  const sections = template.split('===\n').map(section => {
    const paragraphs = section.split('---\n');
    const header = paragraphs.shift()!.split(',')
    const title = header[0]
    const documentationUrl = header[1]
    return (
      <Section
        deviceScreen={deviceScreen}
        element={element}
        title={title}
        documentationUrl={documentationUrl}
        codeSnippets={paragraphs}
      />
    )
  })

  return (
    <div
      className="flex flex-col gap-5 h-full overflow-y-scroll"
    >
      {element.text || element.resourceId ? (
        <>
          <div className="font-bold">
            Here are some examples of how you can interact with this element:
          </div>
          {sections}
        </>
      ) : (
        <div className="bg-red-100 rounded-md p-5">
          <p>This element does not have any <b>text</b> or <b>resource id</b> associated with it.</p>
          <br/>
          <p>The recommended approach to interact with this element is to first <b>assign it a resource id</b> in code.</p>
        </div>
      )}
    </div>
  )
}

const template = `
Tap,https://maestro.mobile.dev/reference/tap-on-view
---
- tapOn: "[text]"
---
- tapOn:
    id: "[id]"
---
- tapOn:
    point: "[point]"
===
Assertion,https://maestro.mobile.dev/reference/assertions
---
- assertVisible: "[text]"
---
- assertVisible:
    id: "[id]"
===
Conditional,https://maestro.mobile.dev/advanced/conditions
---
- runFlow:
    when:
      visible: "[text]"
    file: Subflow.yaml
---
- runFlow:
    when:
      visible:
        id: "[id]"
    file: Subflow.yaml
`

export default Examples
