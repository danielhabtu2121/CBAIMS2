import React, { useState, useEffect } from 'react';
import {
  Shield,
  Eye,
  EyeOff,
  Lock,
  Mail,
  AlertCircle,
  ArrowRight,
  CheckCircle2,
  XCircle,
  KeyRound,
  RefreshCw,
  X,
  Building2,
  FileCheck,
  CheckSquare,
  ShieldCheck,
  UserCheck,
} from 'lucide-react';
import { UserSession } from '../../types';
import { cimsApi } from '../../api/cimsApi';
import { Button } from '../common/Button';
import { Input } from '../common/Input';

interface LoginScreenProps {
  onLogin: (username: string, password: string) => Promise<boolean | { success: boolean; message?: string }>;
  sessionNotice?: string | null;
  onSelectPersona?: (persona: UserSession) => void;
  availablePersonas?: UserSession[];
}

interface PasswordPolicy {
  minLength: number;
  requireSpecial: boolean;
  requireUppercase: boolean;
  requireDigit: boolean;
  maxFailedAttempts: number;
  lockoutDurationMinutes: number;
}

export const LoginScreen: React.FC<LoginScreenProps> = ({
  onLogin,
  sessionNotice,
  onSelectPersona,
  availablePersonas = [],
}) => {
  const [identifier, setIdentifier] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [error, setError] = useState(sessionNotice || '');
  const [loading, setLoading] = useState(false);

  // Policy state
  const [policy, setPolicy] = useState<PasswordPolicy>({
    minLength: 8,
    requireSpecial: true,
    requireUppercase: true,
    requireDigit: true,
    maxFailedAttempts: 5,
    lockoutDurationMinutes: 15,
  });

  // Forgot password modal state
  const [showForgotModal, setShowForgotModal] = useState(false);
  const [forgotStep, setForgotStep] = useState<'request' | 'reset'>('request');
  const [forgotIdentifier, setForgotIdentifier] = useState('');
  const [resetToken, setResetToken] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [showNewPassword, setShowNewPassword] = useState(false);
  const [forgotLoading, setForgotLoading] = useState(false);
  const [forgotMsg, setForgotMsg] = useState<{ type: 'error' | 'success'; text: string } | null>(null);

  useEffect(() => {
    cimsApi.getPasswordPolicy().then((p) => {
      if (p) setPolicy(p);
    }).catch(() => { });
  }, []);

  useEffect(() => {
    if (sessionNotice) setError(sessionNotice);
  }, [sessionNotice]);

  // Validation helpers
  const hasMinLength = (pwd: string) => pwd.length >= policy.minLength;
  const hasSpecial = (pwd: string) => !policy.requireSpecial || /[^A-Za-z0-9]/.test(pwd);
  const hasUppercase = (pwd: string) => !policy.requireUppercase || /[A-Z]/.test(pwd);
  const hasDigit = (pwd: string) => !policy.requireDigit || /[0-9]/.test(pwd);
  const isPasswordCompliant = (pwd: string) =>
    hasMinLength(pwd) && hasSpecial(pwd) && hasUppercase(pwd) && hasDigit(pwd);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      const res = await onLogin(identifier, password);
      if (typeof res === 'object' && !res.success) {
        setError(res.message || 'Invalid username/email or password.');
      } else if (!res) {
        setError('Invalid credentials or account is locked.');
      }
    } catch (err: any) {
      setError(err?.message || 'Authentication failed. Please verify credentials.');
    } finally {
      setLoading(false);
    }
  };



  const handleRequestReset = async (e: React.FormEvent) => {
    e.preventDefault();
    setForgotMsg(null);
    setForgotLoading(true);
    try {
      const res = await cimsApi.forgotPassword(forgotIdentifier);
      if (res.success) {
        setForgotMsg({
          type: 'success',
          text: res.message || 'Password reset token generated. Proceed to set your new password.',
        });
        setForgotStep('reset');
      } else {
        setForgotMsg({ type: 'error', text: res.message || 'Request could not be processed.' });
      }
    } catch (err: any) {
      setForgotMsg({ type: 'error', text: err?.message || 'Failed to submit reset request.' });
    } finally {
      setForgotLoading(false);
    }
  };

  const handleExecuteReset = async (e: React.FormEvent) => {
    e.preventDefault();
    setForgotMsg(null);
    if (!resetToken.trim()) {
      setForgotMsg({ type: 'error', text: 'Reset token is required.' });
      return;
    }
    if (!isPasswordCompliant(newPassword)) {
      setForgotMsg({ type: 'error', text: 'New password does not fulfill bank security policy.' });
      return;
    }
    if (newPassword !== confirmPassword) {
      setForgotMsg({ type: 'error', text: 'Passwords do not match.' });
      return;
    }

    setForgotLoading(true);
    try {
      const res = await cimsApi.resetPassword(resetToken.trim(), newPassword);
      if (res.success) {
        setForgotMsg({ type: 'success', text: res.message || 'Password reset successfully!' });
        setTimeout(() => {
          setShowForgotModal(false);
          setForgotStep('request');
          setForgotMsg(null);
          setPassword(newPassword);
        }, 1500);
      } else {
        setForgotMsg({ type: 'error', text: res.message || 'Unable to reset password.' });
      }
    } catch (err: any) {
      setForgotMsg({ type: 'error', text: err?.message || 'Error occurred during password reset.' });
    } finally {
      setForgotLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-slate-950 flex items-center justify-center p-4 sm:p-6 lg:p-8 font-sans select-none">
      {/* Background Subtle Ambient Glow */}
      <div className="fixed inset-0 pointer-events-none opacity-40">
        <div className="absolute top-1/4 left-1/4 w-96 h-96 bg-[#00205B] rounded-full blur-3xl" />
        <div className="absolute bottom-1/4 right-1/4 w-96 h-96 bg-[#1B4580] rounded-full blur-3xl" />
      </div>

      {/* Main Split Layout Container */}
      <div className="relative w-full max-w-5xl bg-white rounded-2xl shadow-2xl overflow-hidden border border-slate-200/80 grid grid-cols-1 lg:grid-cols-12 z-10">
        {/* Left Column: Brand Showcase Hero (5 cols on lg) */}
        <div className="lg:col-span-5 bg-[#00205B] text-white p-8 sm:p-10 flex flex-col justify-between relative overflow-hidden border-b lg:border-b-0 lg:border-r border-[#143666]">
          {/* Subtle Decorative Dashen Gold Accent Band */}
          <div className="absolute top-0 left-0 right-0 h-1.5 bg-gradient-to-r from-[#D48806] via-[#F5B853] to-[#D48806]" />

          <div>
            {/* Logo & Bank Title */}
            <div className="flex items-center gap-3.5 mb-8">
              <div className="w-14 h-14 rounded-xl bg-white p-2 shadow-md flex items-center justify-center shrink-0">
                <img
                  src="/dashen-logo.png"
                  alt="Dashen Bank Logo"
                  className="w-full h-full object-contain"
                />
              </div>
              <div>
                <span className="text-base font-extrabold tracking-wide text-white block uppercase">
                  ዳሸን ባንክ · DASHEN BANK
                </span>
                <span className="text-xs font-bold text-[#D48806] uppercase tracking-wider block">
                  Always One Step Ahead
                </span>
              </div>
            </div>

            {/* System Headline */}
            <div className="space-y-3 mb-10">
              <div className="inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full bg-blue-900/80 border border-blue-400/30 text-[#D48806] text-[11px] font-bold tracking-wider uppercase">
                <ShieldCheck className="w-3.5 h-3.5" />
                <span>Enterprise Portal</span>
              </div>
              <h2 className="text-2xl sm:text-3xl font-extrabold text-white leading-tight">
                Collateral &amp; Asset Insurance Management
              </h2>
              <p className="text-xs sm:text-sm text-blue-200/80 leading-relaxed">
                Centralized collateral master registry, automated CBS loan linkage, real-time insurance adequacy, and dual-control approvals for Dashen Bank credit operations.
              </p>
            </div>

            {/* Feature Highlights Checklist */}
            <div className="space-y-3.5">
              <div className="flex items-start gap-3">
                <div className="w-6 h-6 rounded-md bg-[#0E284E] border border-blue-400/20 text-[#D48806] flex items-center justify-center shrink-0 mt-0.5">
                  <Building2 className="w-3.5 h-3.5" />
                </div>
                <div>
                  <h4 className="text-xs font-bold text-white">Collateral Perfection &amp; 360° View</h4>
                  <p className="text-[11px] text-blue-200/70">Movable and immovable asset tracking with automated LTV calculations.</p>
                </div>
              </div>

              <div className="flex items-start gap-3">
                <div className="w-6 h-6 rounded-md bg-[#0E284E] border border-blue-400/20 text-[#D48806] flex items-center justify-center shrink-0 mt-0.5">
                  <FileCheck className="w-3.5 h-3.5" />
                </div>
                <div>
                  <h4 className="text-xs font-bold text-white">100% Insurance Adequacy</h4>
                  <p className="text-[11px] text-blue-200/70">Continuous coverage monitoring and automated multi-tier renewal escalations.</p>
                </div>
              </div>

              <div className="flex items-start gap-3">
                <div className="w-6 h-6 rounded-md bg-[#0E284E] border border-blue-400/20 text-[#D48806] flex items-center justify-center shrink-0 mt-0.5">
                  <CheckSquare className="w-3.5 h-3.5" />
                </div>
                <div>
                  <h4 className="text-xs font-bold text-white">Maker-Checker Dual Control</h4>
                  <p className="text-[11px] text-blue-200/70">Strict segregation of duties across registration, linkage, and release.</p>
                </div>
              </div>
            </div>
          </div>

          {/* Footer Regulatory Tag */}
          <div className="pt-8 mt-8 border-t border-blue-900/60 flex items-center justify-between text-[11px] text-blue-300/70">
            <span>National Bank of Ethiopia (NBE) Compliant</span>
            <span className="font-mono text-[10px] text-blue-400">v2.0-SECURED</span>
          </div>
        </div>

        {/* Right Column: Authentication Form (7 cols on lg) */}
        <div className="lg:col-span-7 p-8 sm:p-12 flex flex-col justify-between bg-white">
          <div>
            {/* Header */}
            <div className="mb-8">
              <h3 className="text-xl sm:text-2xl font-bold text-slate-900 tracking-tight">
                Sign In to Your Account
              </h3>
              <p className="text-xs sm:text-sm text-slate-500 mt-1">
                Enter your bank credentials to access your authorized portfolio.
              </p>
            </div>

            {/* Error Banner */}
            {error && (
              <div className="mb-6 p-3.5 bg-red-50 border border-red-200 text-red-800 rounded-xl text-xs flex items-start gap-3 animate-fadeIn">
                <AlertCircle className="w-5 h-5 text-red-600 shrink-0 mt-0.5" />
                <div>
                  <strong className="block font-bold mb-0.5 text-red-900">Authentication Alert</strong>
                  <span className="text-red-700 leading-relaxed">{error}</span>
                </div>
              </div>
            )}

            {/* Form */}
            <form onSubmit={handleSubmit} className="space-y-5">
              <Input
                label="Staff Username or Corporate Email"
                required
                id="username-input"
                name="username"
                autoComplete="username"
                value={identifier}
                onChange={(e) => setIdentifier(e.target.value)}
                placeholder="e.g. sysadmin or brmgr_user"
                leftIcon={<Mail className="w-4 h-4 text-slate-400" />}
              />

              <div className="space-y-1.5 text-left">
                <div className="flex items-center justify-between">
                  <label
                    htmlFor="password-input"
                    className="block text-xs font-semibold text-slate-700 tracking-tight"
                  >
                    Account Password <span className="text-red-500 font-bold">*</span>
                  </label>
                  <button
                    type="button"
                    onClick={() => {
                      setForgotIdentifier(identifier);
                      setForgotMsg(null);
                      setForgotStep('request');
                      setShowForgotModal(true);
                    }}
                    className="text-xs text-[#1F4E7A] hover:text-[#00205B] font-semibold hover:underline"
                  >
                    Forgot Password?
                  </button>
                </div>

                <div className="relative flex items-center">
                  <div className="absolute left-3 flex items-center pointer-events-none text-slate-400">
                    <Lock className="w-4 h-4" />
                  </div>
                  <input
                    id="password-input"
                    name="password"
                    type={showPassword ? 'text' : 'password'}
                    required
                    autoComplete="current-password"
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                    placeholder="Enter your security password"
                    className="w-full rounded-md border border-slate-300 text-sm py-2 pl-9 pr-10 bg-white text-slate-900 placeholder:text-slate-400 focus:outline-none focus:ring-2 focus:ring-[#00205B]/20 focus:border-[#00205B] transition-all"
                  />
                  <button
                    type="button"
                    onClick={() => setShowPassword(!showPassword)}
                    className="absolute right-3 text-slate-400 hover:text-slate-600 transition-colors p-1"
                    title={showPassword ? 'Hide password' : 'Show password'}
                    aria-label={showPassword ? 'Hide password' : 'Show password'}
                  >
                    {showPassword ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                  </button>
                </div>
              </div>

              {/* Submit CTA Button */}
              <div className="pt-2">
                <Button
                  type="submit"
                  variant="primary"
                  size="lg"
                  fullWidth
                  loading={loading}
                  icon={!loading ? <ArrowRight className="w-4 h-4" /> : undefined}
                  iconPosition="right"
                >
                  {loading ? 'Authenticating Session...' : 'Sign In to CBAIMS'}
                </Button>
              </div>
            </form>


          </div>

          {/* Security Notice */}
          <div className="mt-8 pt-4 border-t border-slate-100 flex items-center justify-center gap-1.5 text-xs text-slate-400 text-center">
            <Shield className="w-3.5 h-3.5 text-[#00205B]" />
            <span>Protected Dashen Bank Banking Platform · All Sessions Fully Audited</span>
          </div>
        </div>
      </div>

      {/* Forgot Password / Reset Password Modal */}
      {showForgotModal && (
        <div
          className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/60 backdrop-blur-xs select-none"
          role="dialog"
          aria-modal="true"
        >
          <div className="relative w-full max-w-md bg-white rounded-2xl shadow-2xl border border-slate-200 overflow-hidden transform transition-all duration-200 animate-fadeIn">
            {/* Modal Header */}
            <div className="px-6 py-4 bg-[#00205B] text-white flex items-center justify-between">
              <div className="flex items-center gap-2.5">
                <div className="w-8 h-8 rounded-lg bg-white/10 flex items-center justify-center">
                  <KeyRound className="w-4 h-4 text-[#D48806]" />
                </div>
                <div>
                  <h3 className="text-sm font-bold text-white tracking-tight">
                    {forgotStep === 'request' ? 'Password Recovery' : 'Set New Security Password'}
                  </h3>
                  <p className="text-[11px] text-blue-200">Dashen Security Services</p>
                </div>
              </div>
              <button
                type="button"
                onClick={() => setShowForgotModal(false)}
                className="text-white/70 hover:text-white hover:bg-white/10 p-1.5 rounded-lg transition-colors"
                aria-label="Close"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            {/* Modal Body */}
            <div className="p-6">
              {forgotMsg && (
                <div
                  className={`p-3 rounded-lg text-xs flex items-start gap-2 mb-4 ${
                    forgotMsg.type === 'success'
                      ? 'bg-emerald-50 border border-emerald-200 text-emerald-800'
                      : 'bg-red-50 border border-red-200 text-red-800'
                  }`}
                >
                  {forgotMsg.type === 'success' ? (
                    <CheckCircle2 className="w-4 h-4 shrink-0 text-emerald-600 mt-0.5" />
                  ) : (
                    <AlertCircle className="w-4 h-4 shrink-0 text-red-600 mt-0.5" />
                  )}
                  <span>{forgotMsg.text}</span>
                </div>
              )}

              {forgotStep === 'request' ? (
                <form onSubmit={handleRequestReset} className="space-y-4">
                  <p className="text-xs text-slate-600">
                    Enter your staff username or corporate bank email. A secure, time-limited reset token will be issued.
                  </p>

                  <Input
                    label="Username or Corporate Email"
                    required
                    value={forgotIdentifier}
                    onChange={(e) => setForgotIdentifier(e.target.value)}
                    placeholder="e.g. brmgr_user or brmgr@bank.com"
                  />

                  <div className="flex items-center justify-end gap-2.5 pt-2">
                    <Button
                      type="button"
                      variant="ghost"
                      onClick={() => setShowForgotModal(false)}
                    >
                      Cancel
                    </Button>
                    <Button
                      type="submit"
                      variant="primary"
                      loading={forgotLoading}
                    >
                      Generate Reset Token
                    </Button>
                  </div>
                </form>
              ) : (
                <form onSubmit={handleExecuteReset} className="space-y-4">
                  <Input
                    label="Reset Security Token"
                    required
                    value={resetToken}
                    onChange={(e) => setResetToken(e.target.value)}
                    placeholder="Paste your issued token"
                    className="font-mono text-xs"
                  />

                  <div className="space-y-1.5">
                    <label className="block text-xs font-semibold text-slate-700">
                      New Password <span className="text-red-500 font-bold">*</span>
                    </label>
                    <div className="relative flex items-center">
                      <input
                        type={showNewPassword ? 'text' : 'password'}
                        required
                        value={newPassword}
                        onChange={(e) => setNewPassword(e.target.value)}
                        placeholder="Enter compliant new password"
                        className="w-full rounded-md border border-slate-300 text-sm py-2 px-3 pr-10 focus:outline-none focus:ring-2 focus:ring-[#00205B]/20 focus:border-[#00205B]"
                      />
                      <button
                        type="button"
                        onClick={() => setShowNewPassword(!showNewPassword)}
                        className="absolute right-3 text-slate-400 hover:text-slate-600"
                      >
                        {showNewPassword ? <EyeOff className="w-4 h-4" /> : <Eye className="w-4 h-4" />}
                      </button>
                    </div>

                    {/* Policy validation checklist */}
                    <div className="p-2.5 bg-slate-50 border border-slate-200 rounded-lg text-[11px] space-y-1 mt-2">
                      <div className={`flex items-center gap-1.5 ${hasMinLength(newPassword) ? 'text-emerald-700 font-medium' : 'text-slate-500'}`}>
                        {hasMinLength(newPassword) ? <CheckCircle2 className="w-3.5 h-3.5 text-emerald-600" /> : <XCircle className="w-3.5 h-3.5 text-slate-400" />}
                        <span>At least {policy.minLength} characters</span>
                      </div>
                      <div className={`flex items-center gap-1.5 ${hasUppercase(newPassword) ? 'text-emerald-700 font-medium' : 'text-slate-500'}`}>
                        {hasUppercase(newPassword) ? <CheckCircle2 className="w-3.5 h-3.5 text-emerald-600" /> : <XCircle className="w-3.5 h-3.5 text-slate-400" />}
                        <span>At least one uppercase letter (A-Z)</span>
                      </div>
                      <div className={`flex items-center gap-1.5 ${hasDigit(newPassword) ? 'text-emerald-700 font-medium' : 'text-slate-500'}`}>
                        {hasDigit(newPassword) ? <CheckCircle2 className="w-3.5 h-3.5 text-emerald-600" /> : <XCircle className="w-3.5 h-3.5 text-slate-400" />}
                        <span>At least one number (0-9)</span>
                      </div>
                      <div className={`flex items-center gap-1.5 ${hasSpecial(newPassword) ? 'text-emerald-700 font-medium' : 'text-slate-500'}`}>
                        {hasSpecial(newPassword) ? <CheckCircle2 className="w-3.5 h-3.5 text-emerald-600" /> : <XCircle className="w-3.5 h-3.5 text-slate-400" />}
                        <span>At least one special character (!@#$%^&*...)</span>
                      </div>
                    </div>
                  </div>

                  <div>
                    <Input
                      label="Confirm New Password"
                      type={showNewPassword ? 'text' : 'password'}
                      required
                      value={confirmPassword}
                      onChange={(e) => setConfirmPassword(e.target.value)}
                      placeholder="Re-enter new password"
                    />
                    {confirmPassword && confirmPassword !== newPassword && (
                      <span className="text-[11px] text-red-600 mt-1 block">Passwords do not match</span>
                    )}
                  </div>

                  <div className="flex items-center justify-between pt-2">
                    <button
                      type="button"
                      onClick={() => setForgotStep('request')}
                      className="text-xs text-[#1F4E7A] hover:underline"
                    >
                      ← Back to request token
                    </button>
                    <div className="flex gap-2">
                      <Button
                        type="button"
                        variant="ghost"
                        onClick={() => setShowForgotModal(false)}
                      >
                        Cancel
                      </Button>
                      <Button
                        type="submit"
                        variant="primary"
                        loading={forgotLoading}
                        disabled={forgotLoading || !isPasswordCompliant(newPassword) || newPassword !== confirmPassword}
                      >
                        Set Password
                      </Button>
                    </div>
                  </div>
                </form>
              )}
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
