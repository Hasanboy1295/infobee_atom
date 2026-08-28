'use client';
import { useParams } from 'next/navigation';
import { RequestDetail } from '@/components/RequestDetail';
export default function CpsrDetailPage() { const params = useParams(); return <RequestDetail type="CPSR" id={Number(params.id)} />; }
