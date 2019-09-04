module.exports = {
  preset: 'ts-jest',
  testEnvironment: 'jsdom',
  setupFilesAfterEnv: ["<rootDir>/src/__tests__/setupTests.ts"],
  testPathIgnorePatterns: ["<rootDir>/build", "<rootDir>/node_modules/", "<rootDir>/src/__tests__/util/", "<rootDir>/src/__tests__/setupTests.ts"],
  moduleNameMapper: {
    "\\.(css|less|scss)$": "identity-obj-proxy"
  }
};
