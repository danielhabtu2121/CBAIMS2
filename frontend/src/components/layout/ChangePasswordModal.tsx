import React, { useState } from 'react';
import { X, Key, Check, AlertCircle, ShieldCheck } from 'lucide-react';
import { Button } from '../common/Button';
import { Input } from '../common/Input';

interface ChangePasswordModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSubmit: (oldPass: string, newPass: string) => Promise<boolean>;
}

export const ChangePasswordModal: React.FC<ChangePasswordModalProps> = ({ isOpen, onClose, onSubmit }) => {
  const [currentPassword, setCurrentPassword] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [confirmPassword, setConfirmPassword] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);
  const [success, setSuccess] = useState(false);

  if (!isOpen) return null;

  const hasMinLength = newPassword.length >= 8;
  const hasUpperCase = /[A-Z]/.test(newPassword);
  const hasLowerCase = /[a-z]/.test(newPassword);
  const hasNumber = /[0-9]/.test(newPassword);
  const hasSpecial = /[^A-Za-z0-9]/.test(newPassword);
  const passwordsMatch = newPassword === confirmPassword && newPassword.length > 0;
  const isPolicySatisfied = hasMinLength && hasUpperCase && hasLowerCase && hasNumber && hasSpecial && passwordsMatch;

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!isPolicySatisfied) {
      setError('Please satisfy all password policy requirements.');
      return;
    }
    setError('');
    setLoading(true);
    try {
      const ok = await onSubmit(currentPassword, newPassword);
      if (ok) {
        setSuccess(true);
        setTimeout(() => {
          setSuccess(false);
          onClose();
        }, 1500);
      } else {
        setError('Incorrect current password.');
      }
    } catch (err: any) {
      setError(err.message || 'Failed to update password.');
    } finally {
      setLoading(false);
    }
  };

  const policyChecklist = [
    { label: 'Minimum 8 characters', satisfied: hasMinLength },
    { label: 'At least one uppercase letter (A-Z)', satisfied: hasUpperCase },
    { label: 'At least one lowercase letter (a-z)', satisfied: hasLowerCase },
    { label: 'At least one number (0-9)', satisfied: hasNumber },
    { label: 'At least one special character (!@#$%^&*)', satisfied: hasSpecial },
    { label: 'Confirm password matches', satisfied: passwordsMatch },
  ];

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/60 backdrop-blur-xs select-none"
      role="dialog"
      aria-modal="true"
    >
      <div className="relative w-full max-w-md bg-white rounded-2xl border border-slate-200 shadow-2xl overflow-hidden transform transition-all duration-200 animate-fadeIn">
        {/* Header */}
        <div className="px-6 py-4 bg-[#00205B] text-white flex items-center justify-between">
          <div className="flex items-center gap-2.5">
            <div className="w-8 h-8 rounded-lg bg-white/10 flex items-center justify-center">
              <Key className="w-4 h-4 text-[#D48806]" />
            </div>
            <div>
              <h3 className="text-sm font-bold text-white tracking-tight">Change User Password</h3>
              <p className="text-[11px] text-blue-200">Dashen Bank Security Lifecycle Policy</p>
            </div>
          </div>
          <button
            onClick={onClose}
            className="text-white/70 hover:text-white hover:bg-white/10 p-1.5 rounded-lg transition-colors"
            aria-label="Close"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        <form onSubmit={handleSubmit} className="p-6 space-y-4">
          {error && (
            <div className="p-3 bg-red-50 border border-red-200 text-red-700 rounded-lg text-xs flex items-center gap-2">
              <AlertCircle className="w-4 h-4 shrink-0 text-red-500" />
              <span>{error}</span>
            </div>
          )}

          {success && (
            <div className="p-3 bg-emerald-50 border border-emerald-200 text-emerald-800 rounded-lg text-xs flex items-center gap-2">
              <ShieldCheck className="w-4 h-4 shrink-0 text-emerald-600" />
              <span>Password successfully updated. Session preserved.</span>
            </div>
          )}

          <Input
            label="Current Password"
            type="password"
            required
            value={currentPassword}
            onChange={(e) => setCurrentPassword(e.target.value)}
            placeholder="Enter your current password"
          />

          <Input
            label="New Password"
            type="password"
            required
            value={newPassword}
            onChange={(e) => setNewPassword(e.target.value)}
            placeholder="Enter strong new password"
          />

          <Input
            label="Confirm New Password"
            type="password"
            required
            value={confirmPassword}
            onChange={(e) => setConfirmPassword(e.target.value)}
            placeholder="Re-type new password"
          />

          {/* Password Policy Requirements Checklist */}
          <div className="p-3 bg-slate-50 border border-slate-200 rounded-xl space-y-1.5">
            <span className="text-[11px] font-bold text-slate-700 uppercase tracking-wider block">
              Policy Requirements:
            </span>
            <div className="grid grid-cols-1 gap-1 text-[11.5px]">
              {policyChecklist.map((item, idx) => (
                <div key={idx} className="flex items-center gap-2">
                  <div
                    className={`w-4 h-4 rounded-full flex items-center justify-center shrink-0 ${item.satisfied ? 'bg-emerald-100 text-emerald-700' : 'bg-slate-200 text-slate-400'
                      }`}
                  >
                    <Check className="w-2.5 h-2.5 stroke-[3]" />
                  </div>
                  <span className={item.satisfied ? 'text-slate-800 font-medium' : 'text-slate-500'}>
                    {item.label}
                  </span>
                </div>
              ))}
            </div>
          </div>

          <div className="pt-2 flex items-center justify-end gap-2.5">
            <Button type="button" variant="ghost" onClick={onClose} disabled={loading}>
              Cancel
            </Button>
            <Button
              type="submit"
              variant="primary"
              loading={loading}
              disabled={!isPolicySatisfied || loading || success}
            >
              Update Password
            </Button>
          </div>
        </form>
      </div>
    </div>
  );
};
