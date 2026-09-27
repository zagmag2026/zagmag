function safeAlias(alias) {
  const value = String(alias || "b");
  if (!/^[A-Za-z_][A-Za-z0-9_]*$/.test(value)) {
    throw new Error("Invalid SQL alias");
  }
  return value;
}

/**
 * One authoritative Booking payment classifier.
 *
 * Rules:
 * - Reserved => no payment status.
 * - Active linked Bill => derive only from Total Received (received_amount)
 *   versus Bill Amount (net_amount). Stored bill.payment_status is deliberately
 *   ignored so stale denormalized text can never make one Booking match two tabs.
 * - No active Bill => Booking Advance 0 => Pending, >0 => Part.
 *
 * The nested CASE makes Pending / Part / Full mutually exclusive.
 */
export function bookingPaymentStatusSql(alias = "b") {
  const b = safeAlias(alias);
  const activeBill = `EXISTS (
    SELECT 1 FROM bills pb
    WHERE pb.booking_id=${b}.id AND pb.status<>'CANCELLED'
  )`;
  const full = `EXISTS (
    SELECT 1 FROM bills pb
    WHERE pb.booking_id=${b}.id AND pb.status<>'CANCELLED'
      AND COALESCE(pb.net_amount,0) > 0
      AND COALESCE(pb.received_amount,0) >= COALESCE(pb.net_amount,0)
  )`;
  const part = `EXISTS (
    SELECT 1 FROM bills pb
    WHERE pb.booking_id=${b}.id AND pb.status<>'CANCELLED'
      AND COALESCE(pb.received_amount,0) > 0
      AND COALESCE(pb.received_amount,0) < COALESCE(pb.net_amount,0)
  )`;

  return `CASE
    WHEN COALESCE(${b}.confirmation_state,'BOOKED')='RESERVED' THEN NULL
    WHEN ${activeBill} THEN
      CASE
        WHEN ${full} THEN 'FULL_AMOUNT_RECEIVED'
        WHEN ${part} THEN 'PART_RECEIVED'
        ELSE 'PENDING'
      END
    WHEN COALESCE(${b}.advance_amount,0)>0 THEN 'PART_RECEIVED'
    ELSE 'PENDING'
  END`;
}
