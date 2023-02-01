get('/events?projectId=maestro-studio-mock', (req, res) => {
  res.json({
    events: [1, 2, 3, 4, 5, 6, 7, 8, 9, 10].map(i => ({
      timestamp: Date.now() - (i * 10000),
      id: i,
      path: `/${i % 2 === 0 ? 'cats' : 'dogs'}`,
      matched: i % 3 === 0,
      statusCode: i % 5 === 0 ? 400 : 200,
      response: i % 3 === 0 ? {mocked: false} : {mocked: true},
      projectId: '37e06c0c-8300-4c46-a81a-2352ca5ff57d',
      sessionId: i < 5 ? 'e959522a-f783-4401-952f-be3fa1a3374f' : '79370c07-665c-405d-957e-afedd9bb7b6b'
    }))
  })
})