import { ReactNode } from "react"


const PointIcon = () => (
  <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" className="w-6">
    <path stroke-linecap="round" stroke-linejoin="round" d="M15.042 21.672L13.684 16.6m0 0l-2.51 2.225.569-9.47 5.227 7.917-3.286-.672zm-7.518-.267A8.25 8.25 0 1120.25 10.5M8.288 14.212A5.25 5.25 0 1117.25 10.5" />
  </svg>
)

const GlobeIcon = () => (
  <svg xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24" stroke-width="1.5" stroke="currentColor" className="w-6">
    <path stroke-linecap="round" stroke-linejoin="round" d="M12 21a9.004 9.004 0 008.716-6.747M12 21a9.004 9.004 0 01-8.716-6.747M12 21c2.485 0 4.5-4.03 4.5-9S14.485 3 12 3m0 18c-2.485 0-4.5-4.03-4.5-9S9.515 3 12 3m0 0a8.997 8.997 0 017.843 4.582M12 3a8.997 8.997 0 00-7.843 4.582m15.686 0A11.953 11.953 0 0112 10.5c-2.998 0-5.74-1.1-7.843-2.918m15.686 0A8.959 8.959 0 0121 12c0 .778-.099 1.533-.284 2.253m0 0A17.919 17.919 0 0112 16.5c-3.162 0-6.133-.815-8.716-2.247m0 0A9.015 9.015 0 013 12c0-1.605.42-3.113 1.157-4.418" />
  </svg>
)

const Button = ( {path, children }: { path: string, children: ReactNode }) => (
  <div 
    className="text-lg p-8 m-4 shadow-md border-2 border-gray-100 hover:bg-slate-50 cursor-pointer flex flex-col items-center text-blue-500 font-semibold"
    onClick={() => window.location.assign(path)}
  >
    {children}
  </div>
)

const WelcomePage = () => {
  return (
    <div className="flex h-full overflow-hidden flex-col justify-center items-center">
      <h1 className="text-3xl">Welcome to Maestro Studio 👋</h1>
      <p className="text-lg mt-4">What do you want to do?</p>

      <div className="flex flex-row mt-8">
        <Button path="/interact"><PointIcon />Interact with my app</Button>
        <Button path="/mock"><GlobeIcon />Mock network for my app</Button>
      </div>
    </div>
  )
}

export default WelcomePage