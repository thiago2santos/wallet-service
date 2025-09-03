# 🌊 Git Workflow Guide

> **Industry-Standard Git Flow for the Wallet Service**

## 🏆 **Recommended Workflow: GitHub Flow + Release Branches**

This is the **most popular workflow** used by 60%+ of companies including GitHub, Spotify, and many fintech companies.

### 🌳 **Branch Structure**

```
main (production-ready)
├── develop (integration)
├── feature/user-authentication
├── feature/payment-integration
├── release/v1.2.0
├── hotfix/critical-security-fix
└── docs/update-readme
```

## 📋 **Branch Types & Purposes**

| Branch Type | Purpose | Lifetime | Merge To |
|-------------|---------|----------|----------|
| `main` | Production-ready code | Permanent | - |
| `develop` | Integration & testing | Permanent | `main` |
| `feature/*` | New features | Temporary | `develop` |
| `release/*` | Release preparation | Temporary | `main` + `develop` |
| `hotfix/*` | Critical production fixes | Temporary | `main` + `develop` |
| `docs/*` | Documentation updates | Temporary | `develop` |

## 🚀 **Workflow Steps**

### 1. **Feature Development**

```bash
# Start from develop
git checkout develop
git pull origin develop

# Create feature branch
git checkout -b feature/user-authentication

# Work on your feature
git add .
git commit -m "feat: add JWT authentication"
git commit -m "test: add authentication tests"
git commit -m "docs: update API documentation"

# Push and create PR
git push -u origin feature/user-authentication
```

**GitHub:** Create PR `feature/user-authentication` → `develop`

### 2. **Release Preparation**

```bash
# Create release branch from develop
git checkout develop
git pull origin develop
git checkout -b release/v1.2.0

# Final preparations
git commit -m "chore: bump version to 1.2.0"
git commit -m "docs: update changelog"

# Push release branch
git push -u origin release/v1.2.0
```

**GitHub:** Create PR `release/v1.2.0` → `main`

### 3. **Production Deployment**

```bash
# After release PR is approved and merged to main
git checkout main
git pull origin main

# Tag the release
git tag -a v1.2.0 -m "Release version 1.2.0"
git push origin v1.2.0

# Merge back to develop
git checkout develop
git merge main
git push origin develop
```

### 4. **Hotfix Process**

```bash
# Create hotfix from main
git checkout main
git pull origin main
git checkout -b hotfix/security-vulnerability

# Fix the issue
git commit -m "fix: resolve security vulnerability CVE-2024-1234"

# Push hotfix
git push -u origin hotfix/security-vulnerability
```

**GitHub:** Create PRs:
- `hotfix/security-vulnerability` → `main`
- `hotfix/security-vulnerability` → `develop`

## 🛡️ **Branch Protection Rules**

### **Main Branch Protection**
```yaml
Branch: main
✅ Require pull request reviews (1 reviewer)
✅ Require status checks:
   - unit-tests
   - static-analysis
   - integration-tests
   - security-scan
✅ Require up-to-date branches
✅ Require signed commits
✅ Include administrators
❌ Allow force pushes
❌ Allow deletions
```

### **Develop Branch Protection**
```yaml
Branch: develop
✅ Require pull request reviews (1 reviewer)
✅ Require status checks:
   - unit-tests
   - static-analysis
✅ Require up-to-date branches
✅ Allow force pushes (for maintainers)
❌ Allow deletions
```

## 🔄 **CI/CD Integration**

### **Automated Workflows**

| Branch | Trigger | Actions |
|--------|---------|---------|
| `feature/*` | PR to `develop` | Unit tests, code quality checks |
| `develop` | Push | Full test suite, deploy to staging |
| `release/*` | PR to `main` | Full test suite, security scan |
| `main` | Push | Deploy to production |
| `hotfix/*` | PR to `main` | Emergency pipeline, fast deployment |

### **Environment Mapping**

```
feature/* → Development (local)
develop   → Staging Environment
main      → Production Environment
```

## 📝 **Commit Message Convention**

Use [Conventional Commits](https://www.conventionalcommits.org/):

```bash
# Features
git commit -m "feat: add user authentication endpoint"
git commit -m "feat(api): implement wallet balance query"

# Bug fixes
git commit -m "fix: resolve null pointer in payment processing"
git commit -m "fix(security): patch SQL injection vulnerability"

# Documentation
git commit -m "docs: update API documentation"
git commit -m "docs(readme): add deployment instructions"

# Tests
git commit -m "test: add integration tests for transfers"
git commit -m "test(unit): improve mutation test coverage"

# Chores
git commit -m "chore: update dependencies"
git commit -m "chore(ci): optimize GitHub Actions workflow"
```

## 🎯 **Best Practices**

### **Feature Development**
- ✅ Keep feature branches small and focused
- ✅ Rebase frequently to stay up-to-date
- ✅ Write descriptive commit messages
- ✅ Include tests with your features
- ✅ Update documentation

### **Code Review**
- ✅ Review for functionality, security, and performance
- ✅ Check test coverage and quality
- ✅ Verify documentation updates
- ✅ Ensure CI/CD passes
- ✅ Use GitHub's suggestion feature

### **Release Management**
- ✅ Use semantic versioning (v1.2.3)
- ✅ Maintain a CHANGELOG.md
- ✅ Tag releases consistently
- ✅ Test releases in staging first
- ✅ Have rollback plans ready

## 🚨 **Emergency Procedures**

### **Critical Hotfix**
```bash
# 1. Create hotfix branch
git checkout main
git checkout -b hotfix/critical-issue

# 2. Fix and test locally
git commit -m "fix: resolve critical production issue"

# 3. Fast-track PR (skip some checks if needed)
gh pr create --title "HOTFIX: Critical production issue" --body "Emergency fix"

# 4. Deploy immediately after merge
# (Automated via GitHub Actions)
```

### **Rollback Procedure**
```bash
# 1. Revert the problematic commit
git checkout main
git revert <commit-hash>
git push origin main

# 2. Or rollback to previous tag
git checkout main
git reset --hard v1.1.0
git push --force-with-lease origin main
```

## 📊 **Workflow Metrics**

Track these metrics for continuous improvement:

- **Lead Time:** Feature start → Production
- **Deployment Frequency:** How often you deploy
- **Change Failure Rate:** % of deployments causing issues
- **Recovery Time:** Time to fix production issues

## 🛠️ **Setup Commands**

### **Initial Repository Setup**
```bash
# Clone and setup
git clone https://github.com/thiago2santos/wallet-service.git
cd wallet-service

# Create develop branch
git checkout -b develop
git push -u origin develop

# Set up branch protection (manual in GitHub UI)
# Go to Settings → Branches → Add rule
```

### **Developer Onboarding**
```bash
# Setup local environment
git clone https://github.com/thiago2santos/wallet-service.git
cd wallet-service

# Install git hooks (optional)
git config core.hooksPath .githooks

# Set up aliases
git config alias.co checkout
git config alias.br branch
git config alias.ci commit
git config alias.st status
```

## 📚 **Additional Resources**

- [GitHub Flow Documentation](https://docs.github.com/en/get-started/quickstart/github-flow)
- [Conventional Commits](https://www.conventionalcommits.org/)
- [Semantic Versioning](https://semver.org/)
- [Git Best Practices](https://git-scm.com/book/en/v2)

---

## 🎯 **Quick Reference**

### **Daily Commands**
```bash
# Start new feature
git checkout develop && git pull && git checkout -b feature/my-feature

# Update feature branch
git checkout develop && git pull && git checkout feature/my-feature && git rebase develop

# Finish feature
git push -u origin feature/my-feature
# Then create PR via GitHub UI

# Clean up after merge
git checkout develop && git pull && git branch -d feature/my-feature
```

This workflow ensures **high code quality**, **fast deployment**, and **reliable releases** for your wallet service! 🚀
