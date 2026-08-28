'use client';
import { useParams } from 'next/navigation';
import { RequestDetail } from '@/components/RequestDetail';
export default function AtomDetailPage() { const params = useParams(); return <RequestDetail type="ATOM" id={Number(params.id)} />; }
