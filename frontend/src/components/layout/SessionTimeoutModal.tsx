import React, { useState, useEffect } from 'react';
import { Clock, ShieldAlert } from 'lucide-react';
import { Button } from '../common/Button';

interface SessionTimeoutModalProps {
  isOpen: boolean;
  onStayLoggedIn: () => void;
  onLogout: () => void;
  secondsRemaining?: number;
}

export const SessionTimeoutModal: React.FC<SessionTimeoutModalProps> = ({
  isOpen,
  onStayLoggedIn,
  onLogout,
  secondsRemaining = 120,
}) => {
  const [countdown, setCountdown] = useState(secondsRemaining);

  useEffect(() => {
    if (!isOpen) {
      setCountdown(secondsRemaining);
      return;
    }
    const interval = setInterval(() => {
      setCountdown((prev) => {
        if (prev <= 1) {
          clearInterval(interval);
          onLogout();
          return 0;
        }
        return prev - 1;
      });
    }, 1000);

    return () => clearInterval(interval);
  }, [isOpen, secondsRemaining, onLogout]);

  if (!isOpen) return null;

  const minutes = Math.floor(countdown / 60);
  const seconds = countdown % 60;

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/60 backdrop-blur-xs select-none"
      role="dialog"
      aria-modal="true"
      aria-labelledby="session-timeout-title"
    >
      <div className="relative w-full max-w-md bg-white rounded-2xl border border-slate-200 shadow-2xl p-6 text-center transform transition-all duration-200 animate-fadeIn">
        {/* Warning Icon Badge */}
        <div className="w-14 h-14 rounded-2xl bg-amber-50 border border-amber-200 text-amber-600 flex items-center justify-center mx-auto mb-4 shadow-xs">
          <Clock className="w-7 h-7 stroke-[2.2]" />
        </div>

        <h3 id="session-timeout-title" className="text-lg font-bold text-slate-900 tracking-tight mb-2">
          Session Inactivity Warning
        </h3>

        <p className="text-sm text-slate-600 leading-relaxed mb-6">
          Your active CBAIMS session will automatically terminate in{' '}
          <span className="font-mono font-bold text-red-600 text-base px-2 py-0.5 bg-red-50 rounded border border-red-200">
            {minutes}:{seconds < 10 ? `0${seconds}` : seconds}
          </span>{' '}
          due to inactivity per Dashen Bank security policy.
        </p>

        <div className="flex items-center gap-3">
          <Button
            variant="secondary"
            fullWidth
            onClick={onLogout}
          >
            Log Out Now
          </Button>
          <Button
            variant="primary"
            fullWidth
            onClick={onStayLoggedIn}
          >
            Stay Logged In
          </Button>
        </div>

        <div className="mt-4 flex items-center justify-center gap-1.5 text-[11px] text-slate-400">
          <ShieldAlert className="w-3.5 h-3.5" />
          <span>Automated security protection enabled</span>
        </div>
      </div>
    </div>
  );
};
