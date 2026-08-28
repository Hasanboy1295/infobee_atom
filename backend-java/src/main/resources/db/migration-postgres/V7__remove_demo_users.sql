DELETE FROM users
WHERE username IN ('admin', 'infobee')
  AND NOT EXISTS (SELECT 1 FROM atom_requests WHERE owner_id = users.id)
  AND NOT EXISTS (SELECT 1 FROM cpsr_requests WHERE owner_id = users.id)
  AND NOT EXISTS (SELECT 1 FROM request_comments WHERE author_id = users.id)
  AND NOT EXISTS (SELECT 1 FROM request_history WHERE actor_id = users.id);

UPDATE users
SET enabled = FALSE
WHERE username IN ('admin', 'infobee');
