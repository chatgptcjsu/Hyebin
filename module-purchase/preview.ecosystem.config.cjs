module.exports = {
  apps: [
    {
      name: 'purchase-preview',
      script: 'node',
      args: 'preview-server.cjs',
      cwd: '/home/user/webapp/module-purchase',
      env: {
        NODE_ENV: 'development',
        PORT: 3000
      },
      watch: false,
      instances: 1,
      exec_mode: 'fork'
    }
  ]
}
