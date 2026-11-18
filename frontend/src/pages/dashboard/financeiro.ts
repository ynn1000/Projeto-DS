import type { Lancamento } from '../../services/dashboardService';

export const moeda = new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' });
const doisDigitos = (valor: number) => String(valor).padStart(2, '0');

export function dataLocalISO(data = new Date()) {
    return `${data.getFullYear()}-${doisDigitos(data.getMonth() + 1)}-${doisDigitos(data.getDate())}`;
}

export function resumirMeses(transacoes: Lancamento[], referencia = new Date()) {
    const meses = Array.from({ length: 6 }, (_, indice) => {
        const data = new Date(referencia.getFullYear(), referencia.getMonth() - 5 + indice, 1);
        const rotulo = new Intl.DateTimeFormat('pt-BR', { month: 'short' }).format(data).replace('.', '');
        return {
            chave: dataLocalISO(data).slice(0, 7),
            rotulo: rotulo[0].toUpperCase() + rotulo.slice(1),
            ano: data.getFullYear(),
            receitas: 0,
            despesas: 0,
        };
    });
    const porMes = new Map(meses.map(mes => [mes.chave, mes]));

    for (const transacao of transacoes) {
        const mes = porMes.get(transacao.dataMovimento.slice(0, 7));
        if (!mes) continue;
        const centavos = Math.round(transacao.valor * 100);
        if (transacao.tipo === 'RECEITA') mes.receitas += centavos;
        else if (transacao.tipo === 'DESPESA') mes.despesas += centavos;
    }

    return meses.map(mes => ({
        ...mes,
        receitas: mes.receitas / 100,
        despesas: mes.despesas / 100,
        liquido: (mes.receitas - mes.despesas) / 100,
    }));
}
