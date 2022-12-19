import ReplView from './ReplView';
import { fakeApi } from './fixtures';

export default {
  title: 'ReplView'
}

export const Main = () => {
  return (
    <ReplView api={fakeApi} />
  )
}
