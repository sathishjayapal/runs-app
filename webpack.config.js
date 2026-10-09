const path = require('path');
const TerserPlugin = require('terser-webpack-plugin');
const CopyWebpackPlugin = require('copy-webpack-plugin');
const CssMinimizerPlugin = require('css-minimizer-webpack-plugin');
const Dotenv = require('dotenv-webpack');
const HtmlWebpackPlugin = require('html-webpack-plugin');
const MiniCssExtractPlugin = require('mini-css-extract-plugin');
const TsconfigPathsPlugin = require('tsconfig-paths-webpack-plugin');
const WarningsToErrorsPlugin = require('warnings-to-errors-webpack-plugin');
const fs = require('fs');

/**
 * Backend URL for the local dev server proxy (npm run devserver only).
 * Single source of truth: SERVER_PORT in .env.local, the same value Spring Boot uses locally.
 * BACKEND_URL overrides it. Production builds never use this: on the VM, Spring Boot serves
 * the bundle itself, so the browser calls /api on the same origin.
 */
function resolveBackendUrl() {
  if (process.env.BACKEND_URL) {
    return process.env.BACKEND_URL;
  }
  let port = process.env.SERVER_PORT;
  const localEnv = path.resolve(__dirname, '.env.local');
  if (!port && fs.existsSync(localEnv)) {
    const match = fs.readFileSync(localEnv, 'utf8').match(/^\s*SERVER_PORT\s*=\s*(\d+)\s*$/m);
    port = match && match[1];
  }
  return `http://localhost:${port || 8080}`;
}


module.exports = (env, argv) => ({
  entry: {
    bundle: 'index.tsx'
  },
  output: {
    path: path.resolve(__dirname, './target/classes/static'),
    filename: 'js/[name].js',
    publicPath: '/'
  },
  devtool: argv.mode === 'production' ? false : 'eval-source-map',
  performance: {
    maxEntrypointSize: 488000,
    maxAssetSize: 488000
  },
  optimization: {
    minimize: true,
    minimizer: [
      new TerserPlugin(),
      new CssMinimizerPlugin()
    ]
  },
  plugins: [
    new MiniCssExtractPlugin({
      filename: 'css/[name].css'
    }),
    new Dotenv({
      path: argv.mode === 'production' ? '.env' : '.env.development'
    }),
    new HtmlWebpackPlugin({
      template: './src/main/webapp/index.html'
    }),
    new CopyWebpackPlugin({
      patterns: [{
        from: './src/main/webapp/public'
      }],
    }),
    new WarningsToErrorsPlugin()
  ],
  module: {
    rules: [
      {
        test: /\.tsx?$/,
        use: ['ts-loader']
      },
      {
        test: /\.css$/,
        use: [
          argv.mode === 'production' ? MiniCssExtractPlugin.loader : 'style-loader',
          {
            loader: 'css-loader',
            options: {
              importLoaders: 1
            }
          },
          {
            loader: 'postcss-loader',
            options: {
              postcssOptions: {
                plugins: [
                  require('@tailwindcss/postcss')
                ]
              }
            }
          }
        ]
      },
      {
        test: /\.(woff2|woff|ttf|png|jpg|jpeg|gif|svg|webp)$/,
        type: 'asset'
      }
    ]
  },
  resolve: {
    plugins: [new TsconfigPathsPlugin({})],
    extensions: ['.js', '.jsx', '.ts', '.tsx', '.json']
  },
  devServer: {
    port: 3000,
    compress: true,
    historyApiFallback: {
      disableDotRule: true
    },
    hot: true,
    static: false,
    // Keep the browser same-origin, exactly like the VM: proxy backend paths to Spring Boot.
    // Leave API_PATH empty in .env.development so axios uses relative URLs.
    proxy: [
      {
        context: ['/api', '/login', '/logout', '/actuator', '/swagger-ui', '/v3/api-docs'],
        target: resolveBackendUrl(),
        // GET /login is the React login page; only the form POST goes to Spring Security.
        bypass: (req) => (req.method === 'GET' && req.path === '/login' ? '/index.html' : undefined),
      },
    ],
    watchFiles: [
      'src/main/webapp/**',
    ]
  }
});
