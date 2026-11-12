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
  X
} from 'lucide-react';
import { UserSession } from '../../types';
import { cimsApi } from '../../api/cimsApi';

interface LoginScreenProps {
  onLogin: (username: string, password: string) => Promise<boolean | { success: boolean; message?: string }>;
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
}) => {
  const [identifier, setIdentifier] = useState('');
  const [password, setPassword] = useState('');
  const [showPassword, setShowPassword] = useState(false);
  const [error, setError] = useState('');
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
          text: res.message || 'Password reset instructions have been generated.',
        });
        if (res.simulationResetToken) {
          setResetToken(res.simulationResetToken);
        }
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
    <div
      style={{
        minHeight: '100vh',
        backgroundColor: '#0A1C33',
        backgroundImage: 'radial-gradient(circle at 1px 1px, #1E3A5F 1px, transparent 0)',
        backgroundSize: '28px 28px',
        display: 'flex',
        flexDirection: 'column',
        alignItems: 'center',
        justifyContent: 'center',
        padding: '24px',
        fontFamily: 'Inter, system-ui, sans-serif',
      }}
    >
      <div
        style={{
          width: '440px',
          maxWidth: '100%',
          backgroundColor: '#FFFFFF',
          borderRadius: '14px',
          border: '1px solid #C7D7E8',
          boxShadow: '0 20px 40px -15px rgba(0, 20, 50, 0.35)',
          overflow: 'hidden',
        }}
      >
        {/* Brand Header */}
        <div
          style={{
            backgroundColor: '#00205B',
            padding: '28px 24px 22px 24px',
            textAlign: 'center',
            color: '#FFFFFF',
            position: 'relative',
          }}
        >
          <div
            style={{
              width: '84px',
              height: '84px',
              borderRadius: '16px',
              backgroundColor: '#FFFFFF',
              boxShadow: '0 4px 14px rgba(0,0,0,0.25)',
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              margin: '0 auto 14px auto',
              padding: '6px',
              overflow: 'hidden',
            }}
          >
            <img
              src="/dashen-logo.png"
              alt="Dashen Bank Logo"
              style={{
                width: '100%',
                height: '100%',
                objectFit: 'contain',
              }}
            />
          </div>
          <h1 style={{ fontSize: '19px', fontWeight: 800, margin: 0, letterSpacing: '0.4px' }}>
            ዳሸን ባንክ · DASHEN BANK
          </h1>
          <p style={{ fontSize: '11px', color: '#F7A800', fontWeight: 700, margin: '5px 0 0 0', letterSpacing: '0.08em', textTransform: 'uppercase' }}>
            Collateral Insurance Management System (CIMS)
          </p>
        </div>

        {/* Form Body */}
        <div style={{ padding: '26px 24px' }}>
          {error && (
            <div
              style={{
                padding: '11px 13px',
                backgroundColor: '#FEE2E2',
                border: '1px solid #FCA5A5',
                color: '#991B1B',
                borderRadius: '8px',
                fontSize: '12px',
                marginBottom: '18px',
                display: 'flex',
                alignItems: 'flex-start',
                gap: '8px',
                lineHeight: 1.4,
              }}
            >
              <AlertCircle style={{ width: '16px', height: '16px', flexShrink: 0, marginTop: '2px' }} />
              <div>
                <strong style={{ display: 'block', marginBottom: '2px' }}>Authentication Alert</strong>
                <span>{error}</span>
              </div>
            </div>
          )}

          <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
            <div>
              <label style={{ display: 'block', fontSize: '12px', fontWeight: 700, color: '#0F172A', marginBottom: '6px' }}>
                Username or Corporate Email <span style={{ color: '#DC2626' }}>*</span>
              </label>
              <div style={{ position: 'relative' }}>
                <Mail style={{ position: 'absolute', left: '12px', top: '10px', width: '16px', height: '16px', color: '#64748B' }} />
                <input
                  type="text"
                  required
                  value={identifier}
                  onChange={(e) => setIdentifier(e.target.value)}
                  placeholder="e.g. brmgr_user or brmgr@bank.com"
                  style={{
                    width: '100%',
                    padding: '9px 12px 9px 36px',
                    borderRadius: '7px',
                    border: '1px solid #CBD5E1',
                    fontSize: '13px',
                    outline: 'none',
                    transition: 'border-color 0.15s',
                  }}
                  onFocus={(e) => (e.target.style.borderColor = '#1E40AF')}
                  onBlur={(e) => (e.target.style.borderColor = '#CBD5E1')}
                />
              </div>
            </div>

            <div>
              <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '6px' }}>
                <label style={{ fontSize: '12px', fontWeight: 700, color: '#0F172A' }}>
                  Account Password <span style={{ color: '#DC2626' }}>*</span>
                </label>
                <button
                  type="button"
                  onClick={() => {
                    setForgotIdentifier(identifier);
                    setForgotMsg(null);
                    setForgotStep('request');
                    setShowForgotModal(true);
                  }}
                  style={{
                    background: 'none',
                    border: 'none',
                    fontSize: '11px',
                    color: '#2563EB',
                    cursor: 'pointer',
                    fontWeight: 600,
                    padding: 0,
                  }}
                >
                  Forgot Password?
                </button>
              </div>
              <div style={{ position: 'relative' }}>
                <Lock style={{ position: 'absolute', left: '12px', top: '10px', width: '16px', height: '16px', color: '#64748B' }} />
                <input
                  type={showPassword ? 'text' : 'password'}
                  required
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  placeholder="Enter your security password"
                  style={{
                    width: '100%',
                    padding: '9px 36px 9px 36px',
                    borderRadius: '7px',
                    border: '1px solid #CBD5E1',
                    fontSize: '13px',
                    outline: 'none',
                    transition: 'border-color 0.15s',
                  }}
                  onFocus={(e) => (e.target.style.borderColor = '#1E40AF')}
                  onBlur={(e) => (e.target.style.borderColor = '#CBD5E1')}
                />
                <button
                  type="button"
                  onClick={() => setShowPassword(!showPassword)}
                  style={{
                    position: 'absolute',
                    right: '10px',
                    top: '8px',
                    background: 'transparent',
                    border: 'none',
                    color: '#64748B',
                    cursor: 'pointer',
                    padding: '2px',
                  }}
                  title={showPassword ? 'Hide password' : 'Show password'}
                >
                  {showPassword ? <EyeOff style={{ width: '16px', height: '16px' }} /> : <Eye style={{ width: '16px', height: '16px' }} />}
                </button>
              </div>
            </div>

            <button
              type="submit"
              disabled={loading}
              style={{
                width: '100%',
                padding: '11px',
                borderRadius: '8px',
                backgroundColor: loading ? '#64748B' : '#00205B',
                color: '#FFFFFF',
                fontSize: '14px',
                fontWeight: 700,
                border: 'none',
                cursor: loading ? 'not-allowed' : 'pointer',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                gap: '8px',
                marginTop: '6px',
                transition: 'background-color 0.15s',
              }}
            >
              {loading ? (
                <>
                  <RefreshCw style={{ width: '16px', height: '16px', animation: 'spin 1s linear infinite' }} />
                  <span>Verifying Credentials...</span>
                </>
              ) : (
                <>
                  <Shield style={{ width: '16px', height: '16px' }} />
                  <span>Sign In to CIMS</span>
                </>
              )}
            </button>
          </form>
        </div>

        {/* Footer */}
        <div
          style={{
            padding: '12px 24px',
            backgroundColor: '#F8FAFC',
            borderTop: '1px solid #E2E8F0',
            textAlign: 'center',
            fontSize: '10.5px',
            color: '#64748B',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            gap: '6px',
          }}
        >
          <Shield style={{ width: '13px', height: '13px', color: '#00205B' }} />
          <span>Protected Dashen Bank Banking Platform · All Sessions Fully Audited</span>
        </div>
      </div>

      {/* Forgot Password / Reset Password Modal */}
      {showForgotModal && (
        <div
          style={{
            position: 'fixed',
            inset: 0,
            backgroundColor: 'rgba(0, 0, 0, 0.6)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            zIndex: 100,
            padding: '16px',
          }}
        >
          <div
            style={{
              width: '460px',
              maxWidth: '100%',
              backgroundColor: '#FFFFFF',
              borderRadius: '12px',
              boxShadow: '0 25px 50px -12px rgba(0, 0, 0, 0.25)',
              overflow: 'hidden',
            }}
          >
            {/* Modal Header */}
            <div
              style={{
                padding: '16px 20px',
                backgroundColor: '#00205B',
                color: '#FFFFFF',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'space-between',
              }}
            >
              <div style={{ display: 'flex', alignItems: 'center', gap: '8px' }}>
                <KeyRound style={{ width: '18px', height: '18px', color: '#F7A800' }} />
                <h3 style={{ margin: 0, fontSize: '14px', fontWeight: 700 }}>
                  {forgotStep === 'request' ? 'Password Recovery' : 'Set New Security Password'}
                </h3>
              </div>
              <button
                type="button"
                onClick={() => setShowForgotModal(false)}
                style={{ background: 'none', border: 'none', color: '#CBD5E1', cursor: 'pointer' }}
              >
                <X style={{ width: '18px', height: '18px' }} />
              </button>
            </div>

            {/* Modal Body */}
            <div style={{ padding: '20px' }}>
              {forgotMsg && (
                <div
                  style={{
                    padding: '10px 12px',
                    borderRadius: '6px',
                    fontSize: '12px',
                    marginBottom: '16px',
                    display: 'flex',
                    alignItems: 'flex-start',
                    gap: '8px',
                    backgroundColor: forgotMsg.type === 'success' ? '#ECFDF5' : '#FEF2F2',
                    border: `1px solid ${forgotMsg.type === 'success' ? '#A7F3D0' : '#FECACA'}`,
                    color: forgotMsg.type === 'success' ? '#065F46' : '#991B1B',
                  }}
                >
                  {forgotMsg.type === 'success' ? (
                    <CheckCircle2 style={{ width: '16px', height: '16px', flexShrink: 0, marginTop: '2px' }} />
                  ) : (
                    <AlertCircle style={{ width: '16px', height: '16px', flexShrink: 0, marginTop: '2px' }} />
                  )}
                  <span>{forgotMsg.text}</span>
                </div>
              )}

              {forgotStep === 'request' ? (
                <form onSubmit={handleRequestReset} style={{ display: 'flex', flexDirection: 'column', gap: '14px' }}>
                  <p style={{ fontSize: '12px', color: '#475569', margin: 0 }}>
                    Enter your staff username or corporate bank email. A secure, time-limited reset token will be issued.
                  </p>
                  <div>
                    <label style={{ display: 'block', fontSize: '12px', fontWeight: 600, color: '#0F172A', marginBottom: '4px' }}>
                      Username or Email
                    </label>
                    <input
                      type="text"
                      required
                      value={forgotIdentifier}
                      onChange={(e) => setForgotIdentifier(e.target.value)}
                      placeholder="e.g. brmgr_user or brmgr@bank.com"
                      style={{
                        width: '100%',
                        padding: '8px 12px',
                        borderRadius: '6px',
                        border: '1px solid #CBD5E1',
                        fontSize: '13px',
                        outline: 'none',
                      }}
                    />
                  </div>
                  <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '8px', marginTop: '6px' }}>
                    <button
                      type="button"
                      onClick={() => setShowForgotModal(false)}
                      style={{
                        padding: '8px 14px',
                        borderRadius: '6px',
                        backgroundColor: '#F1F5F9',
                        color: '#475569',
                        border: '1px solid #CBD5E1',
                        fontSize: '12px',
                        fontWeight: 600,
                        cursor: 'pointer',
                      }}
                    >
                      Cancel
                    </button>
                    <button
                      type="submit"
                      disabled={forgotLoading}
                      style={{
                        padding: '8px 16px',
                        borderRadius: '6px',
                        backgroundColor: '#00205B',
                        color: '#FFFFFF',
                        border: 'none',
                        fontSize: '12px',
                        fontWeight: 700,
                        cursor: 'pointer',
                      }}
                    >
                      {forgotLoading ? 'Processing...' : 'Generate Reset Token'}
                    </button>
                  </div>
                </form>
              ) : (
                <form onSubmit={handleExecuteReset} style={{ display: 'flex', flexDirection: 'column', gap: '14px' }}>
                  <div>
                    <label style={{ display: 'block', fontSize: '12px', fontWeight: 600, color: '#0F172A', marginBottom: '4px' }}>
                      Reset Security Token <span style={{ color: '#DC2626' }}>*</span>
                    </label>
                    <input
                      type="text"
                      required
                      value={resetToken}
                      onChange={(e) => setResetToken(e.target.value)}
                      placeholder="Paste your 32-character token"
                      style={{
                        width: '100%',
                        padding: '8px 12px',
                        borderRadius: '6px',
                        border: '1px solid #CBD5E1',
                        fontSize: '12px',
                        fontFamily: 'monospace',
                        outline: 'none',
                      }}
                    />
                  </div>

                  <div>
                    <label style={{ display: 'block', fontSize: '12px', fontWeight: 600, color: '#0F172A', marginBottom: '4px' }}>
                      New Password <span style={{ color: '#DC2626' }}>*</span>
                    </label>
                    <div style={{ position: 'relative' }}>
                      <input
                        type={showNewPassword ? 'text' : 'password'}
                        required
                        value={newPassword}
                        onChange={(e) => setNewPassword(e.target.value)}
                        placeholder="Enter compliant new password"
                        style={{
                          width: '100%',
                          padding: '8px 36px 8px 12px',
                          borderRadius: '6px',
                          border: '1px solid #CBD5E1',
                          fontSize: '13px',
                          outline: 'none',
                        }}
                      />
                      <button
                        type="button"
                        onClick={() => setShowNewPassword(!showNewPassword)}
                        style={{
                          position: 'absolute',
                          right: '10px',
                          top: '8px',
                          background: 'none',
                          border: 'none',
                          color: '#64748B',
                          cursor: 'pointer',
                        }}
                      >
                        {showNewPassword ? <EyeOff style={{ width: '16px', height: '16px' }} /> : <Eye style={{ width: '16px', height: '16px' }} />}
                      </button>
                    </div>

                    {/* Password Policy Validation Checklist */}
                    <div style={{ marginTop: '8px', padding: '8px 10px', backgroundColor: '#F8FAFC', borderRadius: '6px', border: '1px solid #E2E8F0', fontSize: '11px', display: 'flex', flexDirection: 'column', gap: '4px' }}>
                      <div style={{ display: 'flex', alignItems: 'center', gap: '6px', color: hasMinLength(newPassword) ? '#059669' : '#64748B' }}>
                        {hasMinLength(newPassword) ? <CheckCircle2 style={{ width: '12px', height: '12px' }} /> : <XCircle style={{ width: '12px', height: '12px' }} />}
                        <span>At least {policy.minLength} characters</span>
                      </div>
                      <div style={{ display: 'flex', alignItems: 'center', gap: '6px', color: hasUppercase(newPassword) ? '#059669' : '#64748B' }}>
                        {hasUppercase(newPassword) ? <CheckCircle2 style={{ width: '12px', height: '12px' }} /> : <XCircle style={{ width: '12px', height: '12px' }} />}
                        <span>At least one uppercase letter (A-Z)</span>
                      </div>
                      <div style={{ display: 'flex', alignItems: 'center', gap: '6px', color: hasDigit(newPassword) ? '#059669' : '#64748B' }}>
                        {hasDigit(newPassword) ? <CheckCircle2 style={{ width: '12px', height: '12px' }} /> : <XCircle style={{ width: '12px', height: '12px' }} />}
                        <span>At least one digit (0-9)</span>
                      </div>
                      <div style={{ display: 'flex', alignItems: 'center', gap: '6px', color: hasSpecial(newPassword) ? '#059669' : '#64748B' }}>
                        {hasSpecial(newPassword) ? <CheckCircle2 style={{ width: '12px', height: '12px' }} /> : <XCircle style={{ width: '12px', height: '12px' }} />}
                        <span>At least one special character (!@#$%^&*...)</span>
                      </div>
                    </div>
                  </div>

                  <div>
                    <label style={{ display: 'block', fontSize: '12px', fontWeight: 600, color: '#0F172A', marginBottom: '4px' }}>
                      Confirm New Password <span style={{ color: '#DC2626' }}>*</span>
                    </label>
                    <input
                      type={showNewPassword ? 'text' : 'password'}
                      required
                      value={confirmPassword}
                      onChange={(e) => setConfirmPassword(e.target.value)}
                      placeholder="Re-enter new password"
                      style={{
                        width: '100%',
                        padding: '8px 12px',
                        borderRadius: '6px',
                        border: '1px solid #CBD5E1',
                        fontSize: '13px',
                        outline: 'none',
                      }}
                    />
                    {confirmPassword && confirmPassword !== newPassword && (
                      <span style={{ fontSize: '11px', color: '#DC2626', marginTop: '2px', display: 'block' }}>
                        Passwords do not match
                      </span>
                    )}
                  </div>

                  <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginTop: '8px' }}>
                    <button
                      type="button"
                      onClick={() => setForgotStep('request')}
                      style={{
                        background: 'none',
                        border: 'none',
                        color: '#2563EB',
                        fontSize: '12px',
                        cursor: 'pointer',
                        padding: 0,
                      }}
                    >
                      ← Back to request token
                    </button>
                    <div style={{ display: 'flex', gap: '8px' }}>
                      <button
                        type="button"
                        onClick={() => setShowForgotModal(false)}
                        style={{
                          padding: '8px 14px',
                          borderRadius: '6px',
                          backgroundColor: '#F1F5F9',
                          color: '#475569',
                          border: '1px solid #CBD5E1',
                          fontSize: '12px',
                          fontWeight: 600,
                          cursor: 'pointer',
                        }}
                      >
                        Cancel
                      </button>
                      <button
                        type="submit"
                        disabled={forgotLoading || !isPasswordCompliant(newPassword) || newPassword !== confirmPassword}
                        style={{
                          padding: '8px 16px',
                          borderRadius: '6px',
                          backgroundColor: '#00205B',
                          color: '#FFFFFF',
                          border: 'none',
                          fontSize: '12px',
                          fontWeight: 700,
                          cursor: (forgotLoading || !isPasswordCompliant(newPassword) || newPassword !== confirmPassword) ? 'not-allowed' : 'pointer',
                          opacity: (!isPasswordCompliant(newPassword) || newPassword !== confirmPassword) ? 0.6 : 1,
                        }}
                      >
                        {forgotLoading ? 'Updating...' : 'Set Password'}
                      </button>
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
