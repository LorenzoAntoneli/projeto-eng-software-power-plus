window.PP = (() => {
    const STORAGE_KEY = 'powerplus_prototipo_db_v3';

    const DIAS_PT = {
        MONDAY: 'Segunda-feira',
        TUESDAY: 'Terça-feira',
        WEDNESDAY: 'Quarta-feira',
        THURSDAY: 'Quinta-feira',
        FRIDAY: 'Sexta-feira',
        SATURDAY: 'Sábado',
        SUNDAY: 'Domingo'
    };

    const JS_DAY_TO_ENUM = [
        'SUNDAY', 'MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY'
    ];

    const MESES_PT = [
        'janeiro', 'fevereiro', 'março', 'abril', 'maio', 'junho',
        'julho', 'agosto', 'setembro', 'outubro', 'novembro', 'dezembro'
    ];

    const DIAS_CURTOS = ['Dom', 'Seg', 'Ter', 'Qua', 'Qui', 'Sex', 'Sáb'];

    const TIPOS_SERVICO = [
        { codigo: 'INSTALACAO_ELETRICA', descricao: 'Instalação Elétrica Residencial / Comercial', duracaoMinutos: 60 },
        { codigo: 'MANUTENCAO_PREVENTIVA', descricao: 'Manutenção Preventiva e Condomínios', duracaoMinutos: 60 },
        { codigo: 'REPARO_EMERGENCIAL', descricao: 'Reparo de Curto, Queda de Fase ou Disjuntor', duracaoMinutos: 60 },
        { codigo: 'QUADRO_DISTRIBUICAO', descricao: 'Montagem / Adequação de Quadro e Padrão', duracaoMinutos: 60 },
        { codigo: 'VISTORIA_ORCAMENTO', descricao: 'Visita Técnica para Avaliação e Orçamento', duracaoMinutos: 60 }
    ];

    const PERFIS_CLIENTE = [
        { codigo: 'RESIDENCIAL', descricao: 'Residencial' },
        { codigo: 'EMPRESARIAL_CONDOMINIO', descricao: 'Empresarial / Condomínio' }
    ];

    const STATUS_DESC = {
        AGUARDANDO_APROVACAO: 'Aguardando aprovação',
        CONFIRMADO: 'Confirmado',
        EM_ATENDIMENTO: 'Em atendimento',
        CONCLUIDO: 'Concluído',
        RECUSADO: 'Recusado',
        CANCELADO: 'Cancelado',
        AUTOMATICO: 'Triagem automática',
        AGUARDANDO_HUMANO: 'Aguardando eletricista',
        ATENDIDO_HUMANO: 'Respondido pelo eletricista',
        ENCERRADO: 'Encerrado'
    };

    function isoOffsetDays(days) {
        const d = new Date();
        d.setDate(d.getDate() + days);
        const off = d.getTimezoneOffset();
        return new Date(d.getTime() - off * 60000).toISOString().slice(0, 10);
    }

    function proximoDiaUtilOffset(offsetInicial) {
        const d = new Date();
        d.setDate(d.getDate() + offsetInicial);
        if (d.getDay() === 0) d.setDate(d.getDate() + 1);
        const off = d.getTimezoneOffset();
        return new Date(d.getTime() - off * 60000).toISOString().slice(0, 10);
    }

    function hojeIso() {
        return isoOffsetDays(0);
    }

    function amanhaUtilIso() {
        return proximoDiaUtilOffset(1);
    }

    function normalizarFone(t) {
        return String(t || '').replace(/\D/g, '');
    }

    function addUmaHora(hhmm) {
        const [h, m] = hhmm.split(':').map(Number);
        return `${String(h + 1).padStart(2, '0')}:${String(m).padStart(2, '0')}`;
    }

    function criarSeedInicial() {
        const hoje = proximoDiaUtilOffset(0);
        const amanha = proximoDiaUtilOffset(1);
        const depois = proximoDiaUtilOffset(2);
        const bloqueada = proximoDiaUtilOffset(9);

        return {
            seqProtocolo: 105,
            seqId: 50,
            disponibilidades: [
                { id: 1, diaSemana: 'MONDAY', ativo: true, horaInicio: '08:00', horaFim: '18:00', duracaoSlotMinutos: 60 },
                { id: 2, diaSemana: 'TUESDAY', ativo: true, horaInicio: '08:00', horaFim: '18:00', duracaoSlotMinutos: 60 },
                { id: 3, diaSemana: 'WEDNESDAY', ativo: true, horaInicio: '08:00', horaFim: '18:00', duracaoSlotMinutos: 60 },
                { id: 4, diaSemana: 'THURSDAY', ativo: true, horaInicio: '08:00', horaFim: '18:00', duracaoSlotMinutos: 60 },
                { id: 5, diaSemana: 'FRIDAY', ativo: true, horaInicio: '08:00', horaFim: '18:00', duracaoSlotMinutos: 60 },
                { id: 6, diaSemana: 'SATURDAY', ativo: true, horaInicio: '08:00', horaFim: '13:00', duracaoSlotMinutos: 60 },
                { id: 7, diaSemana: 'SUNDAY', ativo: false, horaInicio: '08:00', horaFim: '12:00', duracaoSlotMinutos: 60 }
            ],
            bloqueios: [
                { id: 1, data: bloqueada, motivo: 'Manutenção externa agendada' }
            ],
            clientes: [
                {
                    id: 1,
                    nome: 'Condomínio Residencial Solar',
                    telefone: '1636258899',
                    email: 'sindico@condominiosolar.com.br',
                    enderecoPadrao: 'Av. Presidente Vargas, 1450 - Jardim Sumaré, Ribeirão Preto',
                    perfilCodigo: 'EMPRESARIAL_CONDOMINIO',
                    perfilDescricao: 'Empresarial / Condomínio',
                    observacoes: 'Quadro geral trifásico no subsolo; procurar zelador Sr. Antônio.'
                },
                {
                    id: 2,
                    nome: 'Mariana Ferreira Costa',
                    telefone: '16991452210',
                    email: 'mariana.costa@email.com',
                    enderecoPadrao: 'Rua Conde Afonso Celso, 812, Apto 34 - Jardim Paulista',
                    perfilCodigo: 'RESIDENCIAL',
                    perfilDescricao: 'Residencial',
                    observacoes: 'Portaria 24h.'
                },
                {
                    id: 3,
                    nome: 'Padaria Pão Dourado Ltda',
                    telefone: '1639114400',
                    email: 'contato@paodourado.com.br',
                    enderecoPadrao: 'Rua São José, 1120 - Centro, Ribeirão Preto',
                    perfilCodigo: 'EMPRESARIAL_CONDOMINIO',
                    perfilDescricao: 'Empresarial / Condomínio',
                    observacoes: 'Forno industrial trifásico 220V.'
                }
            ],
            agendamentos: [
                {
                    id: 10,
                    protocolo: 'PP-2026-0101',
                    clienteId: 1,
                    clienteNome: 'Condomínio Residencial Solar',
                    clienteTelefone: '1636258899',
                    clientePerfil: 'Empresarial / Condomínio',
                    data: hoje,
                    horaInicio: '09:00',
                    horaFim: '10:00',
                    tipoServicoCodigo: 'MANUTENCAO_PREVENTIVA',
                    tipoServicoDescricao: 'Manutenção Preventiva e Condomínios',
                    enderecoAtendimento: 'Av. Presidente Vargas, 1450 - Jardim Sumaré, Ribeirão Preto',
                    descricaoServico: 'Inspeção mensal dos barramentos e iluminação de emergência.',
                    statusCodigo: 'CONCLUIDO',
                    statusDescricao: 'Concluído',
                    observacaoTecnica: ' Quadros reapertados e 2 blocos autônomos substituídos.'
                },
                {
                    id: 11,
                    protocolo: 'PP-2026-0102',
                    clienteId: 2,
                    clienteNome: 'Mariana Ferreira Costa',
                    clienteTelefone: '16991452210',
                    clientePerfil: 'Residencial',
                    data: amanha,
                    horaInicio: '10:00',
                    horaFim: '11:00',
                    tipoServicoCodigo: 'INSTALACAO_ELETRICA',
                    clienteEmail: 'mariana.costa@email.com',
                    tipoServicoDescricao: 'Instalação Elétrica Residencial / Comercial',
                    enderecoAtendimento: 'Rua Conde Afonso Celso, 812, Apto 34 - Jardim Paulista',
                    descricaoServico: 'Instalação de circuito dedicado 220V para ar-condicionado split.',
                    statusCodigo: 'CONFIRMADO',
                    statusDescricao: 'Confirmado',
                    observacaoTecnica: ''
                },
                {
                    id: 12,
                    protocolo: 'PP-2026-0103',
                    clienteId: 3,
                    clienteNome: 'Padaria Pão Dourado Ltda',
                    clienteTelefone: '1639114400',
                    clientePerfil: 'Empresarial / Condomínio',
                    data: hoje,
                    horaInicio: '14:00',
                    horaFim: '15:00',
                    tipoServicoCodigo: 'QUADRO_DISTRIBUICAO',
                    tipoServicoDescricao: 'Montagem / Adequação de Quadro e Padrão',
                    enderecoAtendimento: 'Rua São José, 1120 - Centro, Ribeirão Preto',
                    descricaoServico: 'Balanceamento de fases no quadro dos fornos.',
                    statusCodigo: 'CONFIRMADO',
                    statusDescricao: 'Confirmado',
                    observacaoTecnica: ''
                },
                {
                    id: 13,
                    protocolo: 'PP-2026-0104',
                    clienteId: 1,
                    clienteNome: 'Condomínio Residencial Solar',
                    clienteTelefone: '1636258899',
                    clientePerfil: 'Empresarial / Condomínio',
                    data: depois,
                    horaInicio: '08:00',
                    horaFim: '09:00',
                    tipoServicoCodigo: 'VISTORIA_ORCAMENTO',
                    tipoServicoDescricao: 'Visita Técnica para Avaliação e Orçamento',
                    enderecoAtendimento: 'Av. Presidente Vargas, 1450 - Jardim Sumaré, Ribeirão Preto',
                    descricaoServico: 'Avaliação para infraestrutura de recarga veicular nas vagas.',
                    statusCodigo: 'AGUARDANDO_APROVACAO',
                    statusDescricao: 'Aguardando aprovação',
                    observacaoTecnica: ''
                },
                {
                    id: 14,
                    protocolo: 'PP-2026-0105',
                    clienteId: 2,
                    clienteNome: 'Mariana Ferreira Costa',
                    clienteTelefone: '16991452210',
                    clientePerfil: 'Residencial',
                    data: amanha,
                    horaInicio: '16:00',
                    horaFim: '17:00',
                    tipoServicoCodigo: 'REPARO_EMERGENCIAL',
                    tipoServicoDescricao: 'Reparo de Curto, Queda de Fase ou Disjuntor',
                    enderecoAtendimento: 'Rua Conde Afonso Celso, 812, Apto 34 - Jardim Paulista',
                    descricaoServico: 'Tomada da lavanderia aquecendo ao ligar secadora.',
                    statusCodigo: 'AGUARDANDO_APROVACAO',
                    statusDescricao: 'Aguardando aprovação',
                    observacaoTecnica: ''
                }
            ],
            historicoStatus: {
                'PP-2026-0101': [
                    { statusNovo: 'Confirmado', responsavel: 'CLIENTE_WEB', observacao: 'Reserva online realizada' },
                    { statusNovo: 'Concluído', responsavel: 'PROPRIETARIO', observacao: 'Quadros reapertados e testados' }
                ],
                'PP-2026-0102': [
                    { statusNovo: 'Confirmado', responsavel: 'CLIENTE_WEB', observacao: 'Reserva online realizada' }
                ],
                'PP-2026-0103': [
                    { statusNovo: 'Confirmado', responsavel: 'CLIENTE_WEB', observacao: 'Reserva online realizada' }
                ],
                'PP-2026-0104': [
                    { statusNovo: 'Confirmado', responsavel: 'PROPRIETARIO', observacao: 'Visita agendada para o síndico' }
                ]
            },
            conversas: [
                {
                    id: 1,
                    nomeCliente: 'Roberto Almeida (Síndico)',
                    telefoneCliente: '16992341122',
                    statusCodigo: 'AGUARDANDO_HUMANO',
                    statusDescricao: 'Aguardando eletricista',
                    mensagens: [
                        { id: 1, remetente: 'CLIENTE', conteudo: 'Bom dia! Vocês fazem adequação de quadro trifásico antigo em prédio de 12 apartamentos?' },
                        { id: 2, remetente: 'ASSISTENTE', conteudo: 'Realizamos montagem, balanceamento de fases e adequação de quadros residenciais, comerciais e condominiais conforme NBR 5410. Como envolve dimensionamento específico do condomínio, encaminhei sua conversa para retorno direto do eletricista Leonardo.' }
                    ]
                }
            ],
            notificacoes: [
                {
                    id: 1,
                    destinatarioPapel: 'PROPRIETARIO',
                    titulo: 'Cliente aguardando retorno no atendimento',
                    mensagem: 'Roberto Almeida (Síndico) solicitou atendimento humano sobre quadro trifásico.',
                    protocoloReferencia: 'CHAT #1',
                    canalEnvio: 'PAINEL_INTERNO',
                    lida: false
                },
                {
                    id: 2,
                    destinatarioPapel: 'CLIENTE',
                    titulo: 'Confirmação de agendamento',
                    mensagem: 'Visita confirmada para Mariana Ferreira Costa às 10:00.',
                    protocoloReferencia: 'PP-2026-0102',
                    canalEnvio: 'SISTEMA_WEB',
                    lida: true
                }
            ]
        };
    }

    function loadDb() {
        try {
            const raw = localStorage.getItem(STORAGE_KEY);
            if (!raw) {
                const seed = criarSeedInicial();
                localStorage.setItem(STORAGE_KEY, JSON.stringify(seed));
                return seed;
            }
            return JSON.parse(raw);
        } catch (_) {
            const seed = criarSeedInicial();
            localStorage.setItem(STORAGE_KEY, JSON.stringify(seed));
            return seed;
        }
    }

    function saveDb(db) {
        localStorage.setItem(STORAGE_KEY, JSON.stringify(db));
    }

    function resetDb() {
        localStorage.setItem(STORAGE_KEY, JSON.stringify(criarSeedInicial()));
        sessionStorage.removeItem('pp_chat_conversa_id');
    }

    function calcularSlotsLivres(db, isoDate) {
        if (!isoDate || isoDate < hojeIso()) return [];
        if (db.bloqueios.some(b => b.data === isoDate)) return [];

        const d = new Date(isoDate + 'T12:00:00');
        const enumDia = JS_DAY_TO_ENUM[d.getDay()];
        const cfg = db.disponibilidades.find(x => x.diaSemana === enumDia);
        if (!cfg || !cfg.ativo) return [];

        const iniH = Number(cfg.horaInicio.split(':')[0]);
        const fimH = Number(cfg.horaFim.split(':')[0]);

        const ocupados = new Set(
            db.agendamentos
                .filter(a => a.data === isoDate && (a.statusCodigo === 'AGUARDANDO_APROVACAO' || a.statusCodigo === 'CONFIRMADO' || a.statusCodigo === 'EM_ATENDIMENTO'))
                .map(a => a.horaInicio)
        );

        const agora = new Date();
        const ehHoje = isoDate === hojeIso();
        const slots = [];

        for (let h = iniH; h < fimH; h++) {
            const hhmm = `${String(h).padStart(2, '0')}:00`;
            if (ehHoje && h <= agora.getHours()) continue;
            if (!ocupados.has(hhmm)) {
                slots.push(hhmm);
            }
        }
        return slots;
    }

    function gerarRespostaAutomatica(db, texto, solicitarHumano) {
        const t = texto.toLowerCase();
        if (solicitarHumano || t.includes('humano') || t.includes('propriet') || t.includes('leonardo') || t.includes('carlos') || t.includes('eletricista') || t.includes('trifásico') || t.includes('trifasico')) {
            return {
                encaminhar: true,
                resposta: 'Encaminhei sua mensagem diretamente para o eletricista Leonardo com todo o histórico acima. Ele responderá por aqui em instantes.'
            };
        }
        if (t.includes('horário') || t.includes('horario') || t.includes('vaga') || t.includes('dispon')) {
            const dataProx = amanhaUtilIso();
            const livres = calcularSlotsLivres(db, dataProx).slice(0, 5);
            return {
                encaminhar: false,
                resposta: livres.length
                    ? `Para ${formatarDataBr(dataProx)}, temos horários livres às ${livres.join(', ')}. Você pode reservar em menos de 1 minuto na tela "Agendar visita".`
                    : 'Você pode consultar o calendário atualizado por dia diretamente na tela "Agendar visita".'
            };
        }
        if (t.includes('orçamento') || t.includes('orcamento') || t.includes('valor') || t.includes('preço') || t.includes('preco')) {
            return {
                encaminhar: false,
                resposta: 'Serviços pontuais (tomadas, disjuntores e reparos simples) possuem estimativa rápida; instalações completas ou quadros recebem avaliação técnica presencial detalhada antes da execução.'
            };
        }
        if (t.includes('bairro') || t.includes('regi') || t.includes('garantia')) {
            return {
                encaminhar: false,
                resposta: 'Atendemos toda Ribeirão Preto e condomínios da região metropolitana, com garantia de 90 dias sobre a mão de obra seguindo a norma NBR 5410.'
            };
        }
        if (t.includes('serviço') || t.includes('servico') || t.includes('condom')) {
            return {
                encaminhar: false,
                resposta: 'Realizamos instalações residenciais e comerciais, manutenção preventiva para condomínios, reparo de curto/queda de fase e adequação de quadros de distribuição.'
            };
        }
        return {
            encaminhar: false,
            resposta: 'Recebi sua mensagem! Posso informar horários vagos, funcionamento de orçamentos, serviços atendidos ou transferir seu atendimento para o Leonardo clicando no botão ao lado.'
        };
    }

    /* ==================== ROTEADOR LOCAL DO PROTÓTIPO ==================== */
    async function api(url, options = {}) {
        const db = loadDb();
        const method = (options.method || 'GET').toUpperCase();
        const body = options.body ? JSON.parse(options.body) : {};
        const [path, queryStr] = url.split('?');
        const params = new URLSearchParams(queryStr || '');

        if (path.startsWith('/api/admin') && !path.endsWith('/login') && !path.endsWith('/logout')) {
            if (!getToken()) {
                window.location.href = 'login.html';
                throw new Error('Sessão não autenticada.');
            }
        }

        // 1. Catálogo
        if (method === 'GET' && path === '/api/publico/catalogo') {
            return { tiposServico: TIPOS_SERVICO, perfisCliente: PERFIS_CLIENTE };
        }

        // 2. Horários livres
        if (method === 'GET' && path === '/api/publico/horarios-livres') {
            const data = params.get('data');
            return { data, horariosLivres: calcularSlotsLivres(db, data) };
        }

        // 3. Novo agendamento pelo cliente
        if (method === 'POST' && path === '/api/publico/agendamentos') {
            if (!body.nomeCliente || !body.nomeCliente.trim()) throw new Error('Informe o nome completo ou condomínio.');
            const fone = normalizarFone(body.telefone);
            if (fone.length < 10) throw new Error('Informe um telefone válido com DDD.');
            if (!body.endereco || !body.endereco.trim()) throw new Error('Informe o endereço completo da visita.');
            if (!body.data || !body.horaInicio) throw new Error('Selecione a data e um horário disponível.');

            const livres = calcularSlotsLivres(db, body.data);
            if (!livres.includes(body.horaInicio)) {
                throw new Error('Este horário acabou de ser ocupado ou está indisponível. Escolha outro horário.');
            }

            let cliente = db.clientes.find(c => normalizarFone(c.telefone) === fone);
            const perfilObj = PERFIS_CLIENTE.find(p => p.codigo === body.perfilCliente) || PERFIS_CLIENTE[0];
            if (!cliente) {
                cliente = {
                    id: ++db.seqId,
                    nome: body.nomeCliente.trim(),
                    telefone: fone,
                    email: body.email || '',
                    enderecoPadrao: body.endereco.trim(),
                    perfilCodigo: perfilObj.codigo,
                    perfilDescricao: perfilObj.descricao,
                    observacoes: ''
                };
                db.clientes.push(cliente);
            }

            const servObj = TIPOS_SERVICO.find(s => s.codigo === body.tipoServico) || TIPOS_SERVICO[0];
            const protocolo = `PP-2026-0${db.seqProtocolo++}`;
            const novo = {
                id: ++db.seqId,
                protocolo,
                clienteId: cliente.id,
                clienteNome: body.nomeCliente.trim(),
                clienteTelefone: fone,
                clientePerfil: perfilObj.descricao,
                data: body.data,
                horaInicio: body.horaInicio,
                horaFim: addUmaHora(body.horaInicio),
                tipoServicoCodigo: servObj.codigo,
                tipoServicoDescricao: servObj.descricao,
                enderecoAtendimento: body.endereco.trim(),
                descricaoServico: body.descricaoServico || '',
                statusCodigo: 'AGUARDANDO_APROVACAO',
                statusDescricao: 'Aguardando aprovação',
                observacaoTecnica: ''
            };

            db.agendamentos.push(novo);
            db.historicoStatus[protocolo] = [
                { statusNovo: 'Aguardando aprovação', responsavel: 'CLIENTE_WEB', observacao: 'Solicitação registrada — aguardando aprovação do Leonardo' }
            ];
            db.notificacoes.unshift({
                id: ++db.seqId,
                destinatarioPapel: 'PROPRIETARIO',
                titulo: 'Aprovação pendente: ' + novo.clienteNome,
                mensagem: `${novo.clienteNome} solicitou ${ formatarDataBr(novo.data) } às ${novo.horaInicio}. Aprove ou recuse no painel.`,
                protocoloReferencia: protocolo,
                canalEnvio: 'SISTEMA_WEB',
                lida: false
            });
            saveDb(db);
            return novo;
        }

        // 4. Consulta por protocolo
        if (method === 'GET' && path.startsWith('/api/publico/agendamentos/protocolo/')) {
            const prot = decodeURIComponent(path.split('/').pop()).toUpperCase();
            const ag = db.agendamentos.find(a => a.protocolo.toUpperCase() === prot);
            if (!ag) throw new Error(`Protocolo ${prot} não localizado.`);
            return { agendamento: ag, historico: db.historicoStatus[ag.protocolo] || [] };
        }

        // 5. Consulta por telefone
        if (method === 'GET' && path === '/api/publico/agendamentos/cliente') {
            const fone = normalizarFone(params.get('telefone'));
            return db.agendamentos
                .filter(a => normalizarFone(a.clienteTelefone) === fone)
                .sort((a, b) => (b.data + b.horaInicio).localeCompare(a.data + a.horaInicio));
        }

        // 6. Remarcar pelo cliente
        if (method === 'PUT' && path.endsWith('/remarcar')) {
            const partes = path.split('/');
            const prot = decodeURIComponent(partes[partes.length - 2]).toUpperCase();
            const ag = db.agendamentos.find(a => a.protocolo.toUpperCase() === prot);
            if (!ag) throw new Error('Agendamento não encontrado.');
            if (ag.statusCodigo !== 'CONFIRMADO' && ag.statusCodigo !== 'AGUARDANDO_APROVACAO') throw new Error('Apenas pedidos pendentes ou visitas confirmadas podem ser remarcados.');
            if (!body.novaHoraInicio) throw new Error('Selecione um novo horário disponível.');

            ag.data = body.novaData;
            ag.horaInicio = body.novaHoraInicio;
            ag.horaFim = addUmaHora(body.novaHoraInicio);
            ag.statusCodigo = 'AGUARDANDO_APROVACAO';
            ag.statusDescricao = 'Aguardando aprovação';
            (db.historicoStatus[ag.protocolo] = db.historicoStatus[ag.protocolo] || []).push({
                statusNovo: `Remarcado (${formatarDataBr(ag.data)} ${ag.horaInicio})`,
                responsavel: 'CLIENTE_WEB',
                observacao: body.motivo || 'Alteração solicitada pelo cliente'
            });
            db.notificacoes.unshift({
                id: ++db.seqId,
                destinatarioPapel: 'PROPRIETARIO',
                titulo: 'Visita remarcada pelo cliente',
                mensagem: `${ag.clienteNome} alterou ${ag.protocolo} para ${formatarDataBr(ag.data)} às ${ag.horaInicio}.`,
                protocoloReferencia: ag.protocolo,
                canalEnvio: 'SISTEMA_WEB',
                lida: false
            });
            saveDb(db);
            return ag;
        }

        // 7. Cancelar pelo cliente
        if (method === 'POST' && path.endsWith('/cancelar')) {
            const partes = path.split('/');
            const prot = decodeURIComponent(partes[partes.length - 2]).toUpperCase();
            const ag = db.agendamentos.find(a => a.protocolo.toUpperCase() === prot);
            if (!ag) throw new Error('Agendamento não encontrado.');
            ag.statusCodigo = 'CANCELADO';
            ag.statusDescricao = 'Cancelado';
            (db.historicoStatus[ag.protocolo] = db.historicoStatus[ag.protocolo] || []).push({
                statusNovo: 'Cancelado',
                responsavel: 'CLIENTE_WEB',
                observacao: body.motivo || 'Cancelado pelo cliente'
            });
            db.notificacoes.unshift({
                id: ++db.seqId,
                destinatarioPapel: 'PROPRIETARIO',
                titulo: 'Cancelamento de visita',
                mensagem: `Protocolo ${ag.protocolo} cancelado e horário liberado na agenda.`,
                protocoloReferencia: ag.protocolo,
                canalEnvio: 'SISTEMA_WEB',
                lida: false
            });
            saveDb(db);
            return ag;
        }

        // 8. Chat cliente
        if (method === 'POST' && path === '/api/publico/chat/mensagens') {
            let conv = body.conversaId ? db.conversas.find(c => c.id === Number(body.conversaId)) : null;
            if (!conv) {
                conv = {
                    id: ++db.seqId,
                    nomeCliente: body.nomeCliente || 'Visitante Online',
                    telefoneCliente: body.telefoneCliente || '',
                    statusCodigo: 'AUTOMATICO',
                    statusDescricao: STATUS_DESC.AUTOMATICO,
                    mensagens: []
                };
                db.conversas.unshift(conv);
            }
            conv.mensagens.push({ id: ++db.seqId, remetente: 'CLIENTE', conteudo: body.mensagem });

            const bot = gerarRespostaAutomatica(db, body.mensagem, body.solicitarHumano);
            conv.mensagens.push({ id: ++db.seqId, remetente: 'ASSISTENTE', conteudo: bot.resposta });
            if (bot.encaminhar) {
                conv.statusCodigo = 'AGUARDANDO_HUMANO';
                conv.statusDescricao = STATUS_DESC.AGUARDANDO_HUMANO;
                db.notificacoes.unshift({
                    id: ++db.seqId,
                    destinatarioPapel: 'PROPRIETARIO',
                    titulo: 'Atendimento encaminhado no chat',
                    mensagem: `${conv.nomeCliente} solicitou retorno humano.`,
                    protocoloReferencia: `CHAT #${conv.id}`,
                    canalEnvio: 'PAINEL_INTERNO',
                    lida: false
                });
            }
            saveDb(db);
            return conv;
        }

        if (method === 'GET' && path.startsWith('/api/publico/chat/')) {
            const id = Number(path.split('/').pop());
            const conv = db.conversas.find(c => c.id === id);
            if (!conv) throw new Error('Conversa não encontrada.');
            return conv;
        }

        // 9. Login Admin
        if (method === 'POST' && path === '/api/admin/auth/login') {
            if ((body.username === 'leonardo.powerplus' || body.username === 'carlos.powerplus') && body.password === 'PowerPlus@2026') {
                return { token: 'proto-session-ok', username: body.username, nomeExibicao: 'Leonardo' };
            }
            throw new Error('Credenciais inválidas.');
        }

        if (method === 'POST' && path === '/api/admin/auth/logout') {
            return { mensagem: 'Sessão encerrada.' };
        }

        // 10. Dashboard e Agenda Admin
        if (method === 'GET' && path === '/api/admin/dashboard') {
            const dataRef = params.get('data') || hojeIso();
            const doDia = db.agendamentos
                .filter(a => a.data === dataRef)
                .sort((a, b) => a.horaInicio.localeCompare(b.horaInicio));

            const dRef = new Date(dataRef + 'T12:00:00');
            const dow = dRef.getDay();
            const diffSeg = dow === 0 ? -6 : 1 - dow;
            const seg = new Date(dRef);
            seg.setDate(dRef.getDate() + diffSeg);
            const dom = new Date(seg);
            dom.setDate(seg.getDate() + 6);
            const iniIso = seg.toISOString().slice(0, 10);
            const fimIso = dom.toISOString().slice(0, 10);

            const totalSemana = db.agendamentos.filter(
                a => a.data >= iniIso && a.data <= fimIso && a.statusCodigo !== 'CANCELADO'
            ).length;

            const pendentes = db.agendamentos
                .filter(a => a.statusCodigo === 'AGUARDANDO_APROVACAO')
                .sort((a, b) => (a.data + a.horaInicio).localeCompare(b.data + b.horaInicio));

            return {
                dataReferencia: dataRef,
                totalHoje: doDia.length,
                aguardandoAprovacaoHoje: doDia.filter(a => a.statusCodigo === 'AGUARDANDO_APROVACAO').length,
                confirmadosHoje: doDia.filter(a => a.statusCodigo === 'CONFIRMADO').length,
                emAtendimentoHoje: doDia.filter(a => a.statusCodigo === 'EM_ATENDIMENTO').length,
                concluidosHoje: doDia.filter(a => a.statusCodigo === 'CONCLUIDO').length,
                canceladosHoje: doDia.filter(a => a.statusCodigo === 'CANCELADO' || a.statusCodigo === 'RECUSADO').length,
                totalSemana,
                pendentesAprovacaoTotal: pendentes.length,
                chatsAguardandoHumano: db.conversas.filter(c => c.statusCodigo === 'AGUARDANDO_HUMANO').length,
                notificacoesNaoLidas: db.notificacoes.filter(n => !n.lida).length,
                whatsappFeatureFlagAtiva: false,
                pendentesAprovacao: pendentes,
                atendimentosHoje: doDia
            };
        }

        if (method === 'GET' && path === '/api/admin/agenda/semana') {
            const dataRef = params.get('dataReferencia') || hojeIso();
            const dRef = new Date(dataRef + 'T12:00:00');
            const dow = dRef.getDay();
            const diffSeg = dow === 0 ? -6 : 1 - dow;
            const seg = new Date(dRef);
            seg.setDate(dRef.getDate() + diffSeg);
            const sab = new Date(seg);
            sab.setDate(seg.getDate() + 5);
            const iniIso = seg.toISOString().slice(0, 10);
            const fimIso = sab.toISOString().slice(0, 10);

            return {
                inicioSemana: iniIso,
                fimSemana: fimIso,
                agendamentos: db.agendamentos
                    .filter(a => a.data >= iniIso && a.data <= fimIso)
                    .sort((a, b) => (a.data + a.horaInicio).localeCompare(b.data + b.horaInicio))
            };
        }

        if (method === 'POST' && path.endsWith('/aprovar')) {
            const partes = path.split('/');
            const id = Number(partes[partes.length - 2]);
            const ag = db.agendamentos.find(a => a.id === id);
            if (!ag) throw new Error('Atendimento não encontrado.');
            ag.statusCodigo = 'CONFIRMADO';
            ag.statusDescricao = 'Confirmado';
            if (body.observacao) ag.observacaoTecnica = body.observacao;
            (db.historicoStatus[ag.protocolo] = db.historicoStatus[ag.protocolo] || []).push({
                statusNovo: 'Confirmado',
                responsavel: 'Leonardo',
                observacao: body.observacao || 'Visita aprovada pelo Leonardo'
            });
            saveDb(db);
            return ag;
        }

        if (method === 'POST' && path.endsWith('/recusar')) {
            const partes = path.split('/');
            const id = Number(partes[partes.length - 2]);
            const ag = db.agendamentos.find(a => a.id === id);
            if (!ag) throw new Error('Atendimento não encontrado.');
            ag.statusCodigo = 'RECUSADO';
            ag.statusDescricao = 'Recusado';
            ag.observacaoTecnica = body.observacao || 'Horário indisponível para atendimento.';
            (db.historicoStatus[ag.protocolo] = db.historicoStatus[ag.protocolo] || []).push({
                statusNovo: 'Recusado',
                responsavel: 'Leonardo',
                observacao: ag.observacaoTecnica
            });
            saveDb(db);
            return ag;
        }

        if (method === 'PATCH' && path.endsWith('/status')) {
            const partes = path.split('/');
            const id = Number(partes[partes.length - 2]);
            const ag = db.agendamentos.find(a => a.id === id);
            if (!ag) throw new Error('Atendimento não encontrado.');
            ag.statusCodigo = body.novoStatus;
            ag.statusDescricao = STATUS_DESC[body.novoStatus] || body.novoStatus;
            if (body.observacao) ag.observacaoTecnica = body.observacao;
            (db.historicoStatus[ag.protocolo] = db.historicoStatus[ag.protocolo] || []).push({
                statusNovo: ag.statusDescricao,
                responsavel: 'PROPRIETARIO',
                observacao: body.observacao || ''
            });
            saveDb(db);
            return ag;
        }

        if (method === 'POST' && path.endsWith('/lembrete')) {
            const partes = path.split('/');
            const id = Number(partes[partes.length - 2]);
            const ag = db.agendamentos.find(a => a.id === id);
            db.notificacoes.unshift({
                id: ++db.seqId,
                destinatarioPapel: 'CLIENTE',
                titulo: 'Lembrete de visita enviado',
                mensagem: `Lembrete registrado para ${ag.clienteNome} (${formatarDataBr(ag.data)} às ${ag.horaInicio}).`,
                protocoloReferencia: ag.protocolo,
                canalEnvio: 'SISTEMA_INTERNO',
                lida: false
            });
            saveDb(db);
            return { mensagem: `Lembrete registrado para ${ag.clienteNome}.` };
        }

        // 11. Expediente e Bloqueios
        if (method === 'GET' && path === '/api/admin/disponibilidade') {
            return { disponibilidades: db.disponibilidades, bloqueios: db.bloqueios };
        }

        if (method === 'PUT' && path === '/api/admin/disponibilidade') {
            const row = db.disponibilidades.find(d => d.diaSemana === body.diaSemana);
            if (row) {
                row.ativo = Boolean(body.ativo);
                row.horaInicio = body.horaInicio;
                row.horaFim = body.horaFim;
                saveDb(db);
            }
            return row;
        }

        if (method === 'POST' && path === '/api/admin/bloqueios') {
            const conflitos = db.agendamentos.filter(
                a => a.data === body.data && (a.statusCodigo === 'CONFIRMADO' || a.statusCodigo === 'EM_ATENDIMENTO')
            );
            if (conflitos.length > 0 && !body.forcarComConflito) {
                throw new Error(`Já existem ${conflitos.length} visita(s) ativa(s) em ${formatarDataBr(body.data)} (${conflitos.map(c => c.protocolo).join(', ')}). Confirme o bloqueio caso deseje bloquear novos horários.`);
            }
            const novoBloq = { id: ++db.seqId, data: body.data, motivo: body.motivo };
            db.bloqueios.push(novoBloq);
            saveDb(db);
            return novoBloq;
        }

        if (method === 'DELETE' && path.startsWith('/api/admin/bloqueios/')) {
            const id = Number(path.split('/').pop());
            db.bloqueios = db.bloqueios.filter(b => b.id !== id);
            saveDb(db);
            return null;
        }

        // 12. Clientes
        if (method === 'GET' && path === '/api/admin/clientes') {
            const termo = (params.get('termo') || '').toLowerCase().trim();
            if (!termo) return db.clientes;
            return db.clientes.filter(c =>
                c.nome.toLowerCase().includes(termo) || c.telefone.includes(normalizarFone(termo) || termo)
            );
        }

        if (method === 'POST' && path === '/api/admin/clientes') {
            const fone = normalizarFone(body.telefone);
            if (fone.length < 10) throw new Error('Informe o telefone com DDD.');
            if (db.clientes.some(c => normalizarFone(c.telefone) === fone)) {
                throw new Error('Já existe um cliente cadastrado com este telefone.');
            }
            const pObj = PERFIS_CLIENTE.find(p => p.codigo === body.perfil) || PERFIS_CLIENTE[0];
            const novoCli = {
                id: ++db.seqId,
                nome: body.nome.trim(),
                telefone: fone,
                email: body.email || '',
                enderecoPadrao: body.endereco.trim(),
                perfilCodigo: pObj.codigo,
                perfilDescricao: pObj.descricao,
                observacoes: body.observacoes || ''
            };
            db.clientes.unshift(novoCli);
            saveDb(db);
            return novoCli;
        }

        if (method === 'GET' && path.endsWith('/historico') && path.includes('/clientes/')) {
            const partes = path.split('/');
            const id = Number(partes[partes.length - 2]);
            return db.agendamentos.filter(a => a.clienteId === id);
        }

        // 13. Conversas Admin
        if (method === 'GET' && path === '/api/admin/conversas') {
            return db.conversas;
        }

        if (method === 'POST' && path.endsWith('/responder')) {
            const partes = path.split('/');
            const id = Number(partes[partes.length - 2]);
            const conv = db.conversas.find(c => c.id === id);
            conv.mensagens.push({ id: ++db.seqId, remetente: 'PROPRIETARIO', conteudo: body.resposta });
            conv.statusCodigo = body.encerrar ? 'ENCERRADO' : 'ATENDIDO_HUMANO';
            conv.statusDescricao = STATUS_DESC[conv.statusCodigo];
            saveDb(db);
            return conv;
        }

        // 14. Notificações Admin
        if (method === 'GET' && path === '/api/admin/notificacoes') {
            return db.notificacoes;
        }

        if (method === 'POST' && path === '/api/admin/notificacoes/marcar-lidas') {
            db.notificacoes.forEach(n => { n.lida = true; });
            saveDb(db);
            return { ok: true };
        }

        throw new Error(`Rota não suportada: ${path}`);
    }

    /* ==================== UTILITÁRIOS DE SESSÃO E UI ==================== */
    function getToken() {
        return localStorage.getItem('pp_proto_token');
    }

    function setSession(token, nomeExibicao) {
        localStorage.setItem('pp_proto_token', token);
        localStorage.setItem('pp_proto_owner', nomeExibicao || 'Leonardo');
    }

    function clearSession() {
        localStorage.removeItem('pp_proto_token');
        localStorage.removeItem('pp_proto_owner');
    }

    function formatarDataBr(isoDate) {
        if (!isoDate) return '';
        const [y, m, d] = isoDate.split('-');
        return `${d}/${m}/${y}`;
    }

    function formatarDataExtenso(isoDate) {
        if (!isoDate) return '—';
        const d = new Date(isoDate + 'T12:00:00');
        return `${DIAS_CURTOS[d.getDay()]}, ${d.getDate()} de ${MESES_PT[d.getMonth()]} de ${d.getFullYear()}`;
    }

    function esc(str) {
        if (str == null) return '';
        return String(str)
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;');
    }

    function toast(msg, isError = false) {
        let box = document.getElementById('ppToast');
        if (!box) {
            box = document.createElement('div');
            box.id = 'ppToast';
            document.body.appendChild(box);
        }
        box.textContent = msg;
        box.className = isError ? 'toast error' : 'toast';
        clearTimeout(box._timer);
        box._timer = setTimeout(() => box.classList.add('hidden'), 4000);
    }

    function openDialog({ title, description = '', inputLabel = null, defaultValue = '', placeholder = '', confirmLabel = 'Confirmar' }) {
        return new Promise(resolve => {
            let overlay = document.getElementById('ppSharedModal');
            if (overlay) overlay.remove();

            overlay = document.createElement('div');
            overlay.id = 'ppSharedModal';
            overlay.className = 'modal-overlay';
            overlay.innerHTML = `
                <div class="modal-window">
                    <div class="modal-top">
                        <h3>${esc(title)}</h3>
                        <button type="button" class="modal-close" data-close>×</button>
                    </div>
                    ${description ? `<p style="font-size:0.86rem;color:var(--ink-secondary);margin-bottom:14px">${esc(description)}</p>` : ''}
                    <form id="ppDialogForm">
                        ${inputLabel !== null ? `
                            <div class="field">
                                <label>${esc(inputLabel)}</label>
                                <input type="text" id="ppDialogInput" value="${esc(defaultValue)}" placeholder="${esc(placeholder)}">
                            </div>
                        ` : ''}
                        <div class="modal-actions-row">
                            <button type="button" class="btn btn-secondary" data-close>Cancelar</button>
                            <button type="submit" class="btn btn-primary">${esc(confirmLabel)}</button>
                        </div>
                    </form>
                </div>
            `;
            document.body.appendChild(overlay);

            const inp = overlay.querySelector('#ppDialogInput');
            if (inp) inp.focus();

            const close = (val) => {
                overlay.remove();
                resolve(val);
            };

            overlay.querySelectorAll('[data-close]').forEach(b => b.addEventListener('click', () => close(null)));
            overlay.querySelector('#ppDialogForm').addEventListener('submit', ev => {
                ev.preventDefault();
                close(inp ? inp.value : true);
            });
        });
    }

    async function initBackoffice(activeSlug) {
        if (!getToken()) {
            window.location.replace('login.html');
            return false;
        }

        const host = document.getElementById('boSidebarMount');
        if (host) {
            const ownerName = localStorage.getItem('pp_proto_owner') || 'Leonardo';
            host.innerHTML = `
                <div class="bo-brand">
                    <img src="../img/logo-powerplus-light.svg" alt="Power Plus Gestão Operacional">
                </div>

                <div class="bo-section-label">Operação de Campo</div>
                <nav class="bo-nav" aria-label="Navegação operacional">
                    <a href="agenda.html" class="bo-link ${activeSlug === 'agenda' ? 'active' : ''}">
                        <span>Agenda e aprovações</span>
                        <span id="boCountAprovacoes" class="bo-badge">0</span>
                    </a>
                    <a href="expediente.html" class="bo-link ${activeSlug === 'expediente' ? 'active' : ''}">
                        <span>Expediente e bloqueios</span>
                    </a>
                    <a href="clientes.html" class="bo-link ${activeSlug === 'clientes' ? 'active' : ''}">
                        <span>Cadastro de clientes</span>
                    </a>
                </nav>

                <div class="bo-section-label" style="margin-top:8px">Relacionamento</div>
                <nav class="bo-nav" aria-label="Navegação de comunicação">
                    <a href="mensagens.html" class="bo-link ${activeSlug === 'mensagens' ? 'active' : ''}">
                        <span>Mensagens recebidas</span>
                        <span id="boCountChats" class="bo-badge">0</span>
                    </a>
                    <a href="avisos.html" class="bo-link ${activeSlug === 'avisos' ? 'active' : ''}">
                        <span>Registro de avisos</span>
                        <span id="boCountAvisos" class="bo-badge">0</span>
                    </a>
                </nav>

                <div class="bo-footer">
                    <div class="bo-user-row">
                        <strong>${esc(ownerName)} · Responsável Técnico</strong>
                        <button type="button" id="boBtnReset" style="background:none;border:none;color:#8da4c2;font-size:0.73rem;text-decoration:underline;cursor:pointer;padding:0;margin-top:3px">Restaurar dados iniciais</button>
                    </div>
                    <div class="bo-footer-links">
                        <a href="../index.html" target="_blank">Portal do cliente</a>
                        <button type="button" id="boBtnLogout">Encerrar sessão</button>
                    </div>
                </div>
            `;

            document.getElementById('boBtnLogout').addEventListener('click', () => {
                clearSession();
                window.location.href = 'login.html';
            });

            document.getElementById('boBtnReset').addEventListener('click', () => {
                resetDb();
                toast('Dados iniciais restaurados.');
                setTimeout(() => window.location.reload(), 500);
            });
        }

        await refreshSidebarCounters();
        return true;
    }

    async function refreshSidebarCounters() {
        const dash = await api(`/api/admin/dashboard?data=${hojeIso()}`);
        const elAprov = document.getElementById('boCountAprovacoes');
        const elChats = document.getElementById('boCountChats');
        const elAvisos = document.getElementById('boCountAvisos');
        if (elAprov) {
            const pend = dash.pendentesAprovacaoTotal ?? 0;
            elAprov.textContent = pend;
            elAprov.classList.toggle('has-alert', pend > 0);
        }
        if (elChats) {
            elChats.textContent = dash.chatsAguardandoHumano;
            elChats.classList.toggle('has-alert', dash.chatsAguardandoHumano > 0);
        }
        if (elAvisos) {
            elAvisos.textContent = dash.notificacoesNaoLidas;
            elAvisos.classList.toggle('has-alert', dash.notificacoesNaoLidas > 0);
        }
        return dash;
    }

    return {
        DIAS_PT,
        getToken,
        setSession,
        clearSession,
        hojeIso,
        amanhaUtilIso,
        formatarDataBr,
        formatarDataExtenso,
        esc,
        toast,
        api,
        openDialog,
        initBackoffice,
        refreshSidebarCounters
    };
})();

