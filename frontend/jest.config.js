/** @type {import('jest').Config} */
module.exports = {
  preset: 'jest-preset-angular',
  setupFilesAfterEnv: ['<rootDir>/src/setup-jest.ts'],
  testEnvironmentOptions: {
    customExportConditions: ['node', 'jest'],
  },
  testMatch: ['**/?(*.)+(spec).ts'],
  collectCoverage: true,
  coverageDirectory: 'coverage',
  coverageReporters: ['lcov', 'text', 'html'],
  coveragePathIgnorePatterns: [
    '/node_modules/',
    '/dist/',
    '/coverage/',
    '/src/main.ts',
    '/src/polyfills.ts',
    '/src/setup-jest.ts',
    '/src/environments/',
    '\\.config\\.(ts|js)$',
    '\\.d\\.ts$'
  ],
  coverageThreshold: {
    global: {
      branches: 70,
      functions: 70,
      lines: 70,
      statements: 70
    }
  }
};

