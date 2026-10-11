
document.addEventListener('DOMContentLoaded', () => {
    const page = document.body.dataset.page;
    if (document.body.dataset.protected === 'true' && !UTPlay.requireAuth()) return;

    UTPlay.fillUserChrome();

    document.querySelectorAll('.js-logout')
        .forEach(button => button.addEventListener('click', UTPlay.logout));

    setupPasswordToggles();

    const initializers = {
        login: initLogin,
        registro: initRegistro,
        dashboard: initDashboard,
        cursos: initCursos,
        quiz: initQuiz,
        resultado: initResultado,
        perfil: initPerfil
    };

    initializers[page]?.();
});

function setupPasswordToggles() {
    document.querySelectorAll('[data-toggle-password]').forEach(button => {
        button.addEventListener('click', () => {
            const input = document.getElementById(button.dataset.togglePassword);
            const showing = input.type === 'text';
            input.type = showing ? 'password' : 'text';
            button.textContent = showing ? 'Ver' : 'Ocultar';
        });
    });
}

// LOGIN
function initLogin() {
    if (UTPlay.token() && UTPlay.user()) {
        location.replace('/dashboard.html');
        return;
    }

    const form = document.getElementById('loginForm');
    const alert = document.getElementById('formAlert');
    const button = document.getElementById('submitButton');

    if (new URLSearchParams(location.search).get('sesion') === 'expirada') {
        UTPlay.showAlert(alert, 'Tu sesión terminó. Ingresa nuevamente.');
    }

    form.addEventListener('submit', async event => {
        event.preventDefault();
        UTPlay.hideAlert(alert);

        if (!form.reportValidity()) return;

        UTPlay.setLoading(button, true);

        try {
            const response = await UTPlay.api('/usuarios/login', {
                method: 'POST',
                publicRequest: true,
                body: JSON.stringify({
                    correo: form.correo.value.trim(),
                    password: form.password.value
                })
            });

            UTPlay.setAuth(response);
            location.replace('/dashboard.html');

        } catch (error) {
            UTPlay.showAlert(alert, error.message);
        } finally {
            UTPlay.setLoading(button, false);
        }
    });
}

// REGISTRO
function initRegistro() {
    const form = document.getElementById('registerForm');
    const alert = document.getElementById('formAlert');
    const button = document.getElementById('submitButton');

    form.addEventListener('submit', async event => {
        event.preventDefault();
        UTPlay.hideAlert(alert);

        if (!form.reportValidity()) return;

        const payload = {
            nombre: form.nombre.value.trim(),
            correo: form.correo.value.trim(),
            password: form.password.value,
            cicloActual: Number(form.cicloActual.value)
        };

        UTPlay.setLoading(button, true);

        try {
            await UTPlay.api('/usuarios/registrar', {
                method: 'POST',
                publicRequest: true,
                body: JSON.stringify(payload)
            });

            const response = await UTPlay.api('/usuarios/login', {
                method: 'POST',
                publicRequest: true,
                body: JSON.stringify({
                    correo: payload.correo,
                    password: payload.password
                })
            });

            UTPlay.setAuth(response);
            location.replace('/dashboard.html');

        } catch (error) {
            UTPlay.showAlert(alert, error.message);
        } finally {
            UTPlay.setLoading(button, false);
        }
    });
}

// DASHBOARD
async function initDashboard() {
    const cached = UTPlay.user();

    try {
        const [profile, progress, courses] = await Promise.all([
            UTPlay.api(`/usuarios/${cached.idUsuario}`),
            UTPlay.api(`/resultados/${cached.idUsuario}`),
            UTPlay.api('/cursos')
        ]);

        UTPlay.updateUser(profile);

        document.getElementById('welcomeName').textContent =
            UTPlay.firstName(profile.nombre);

        document.getElementById('pointsStat').textContent =
            profile.puntosTotales;

        document.getElementById('levelStat').textContent =
            profile.nivel;

        document.getElementById('quizStat').textContent =
            progress.quizzesCompletados;

        document.getElementById('averageStat').textContent =
            `${progress.promedioMejoresPuntajes}%`;

        renderDashboardCourses(courses);
        renderProgress(progress);

        renderResultsTable(
            document.getElementById('recentResults'),
            progress.historial.slice(0, 5),
            false
        );

    } catch (error) {
        UTPlay.toast(error.message);
    }
}

function renderDashboardCourses(courses) {
    const container = document.getElementById('dashboardCourses');

    const available = courses
        .filter(course =>
            course.disponible &&
            Array.isArray(course.quizzes) &&
            course.quizzes.length
        )
        .slice(0, 3);

    if (!available.length) {
        container.innerHTML =
            '<p class="empty-state">Aún no hay cursos con quizzes disponibles para tu ciclo.</p>';
        return;
    }

    container.innerHTML = available.map(course => {
        const quiz = course.quizzes[0];

        const ciclo = Number(course.ciclo) === 0
            ? 'N'
            : course.ciclo;

        return `
            <article class="course-row">
                <span class="course-number">${ciclo}</span>

                <div>
                    <h3>${UTPlay.escapeHtml(course.nombreCurso)}</h3>

                    <p>
                        ${UTPlay.escapeHtml(quiz.titulo)}
                        · Nota mínima ${quiz.puntajeMinimo}%
                    </p>
                </div>

                <a class="btn btn-soft"
                   href="/quiz.html?id=${encodeURIComponent(quiz.idQuiz)}">
                    Iniciar
                </a>
            </article>
        `;
    }).join('');
}

function renderProgress(progress) {
    const value = progress.porcentajeAvance;

    document.getElementById('progressValue').textContent =
        `${value}%`;

    document.getElementById('progressRing')
        .style.setProperty('--progress', `${value * 3.6}deg`);

    document.getElementById('progressCopy').textContent =
        progress.quizzesDisponibles
            ? `${progress.quizzesCompletados} de ${progress.quizzesDisponibles} quizzes distintos completados hasta tu ciclo.`
            : 'Todavía no hay quizzes disponibles.';
}

// MALLA CURRICULAR
function initCursos() {
    document.getElementById('cycleFilter')
        .addEventListener('change', loadCourses);

    loadCourses();
}

async function loadCourses() {
    const cycle = document.getElementById('cycleFilter').value;
    const container = document.getElementById('coursesGrid');
    const counter = document.getElementById('courseCount');

    container.innerHTML =
        '<div class="skeleton skeleton-course"></div>'.repeat(3);

    counter.textContent = 'Cargando cursos…';

    try {
        const allCourses = await UTPlay.api('/cursos');

        if (!Array.isArray(allCourses)) {
            throw new Error(
                'El servidor no devolvió una lista de cursos.'
            );
        }

        // Incluye Nivelación (ciclo 0)
        const courses = allCourses.filter(course =>
            cycle === '' ||
            Number(course.ciclo) === Number(cycle)
        );

        // Ordenar por ciclo y nombre
        courses.sort((a, b) =>
            Number(a.ciclo) - Number(b.ciclo) ||
            String(a.nombreCurso ?? '').localeCompare(
                String(b.nombreCurso ?? ''),
                'es'
            )
        );

        counter.textContent =
            `${courses.length} curso${courses.length === 1 ? '' : 's'} encontrado${courses.length === 1 ? '' : 's'}`;

        renderCoursesGrid(courses);

    } catch (error) {
        counter.textContent = 'No se pudieron cargar los cursos';

        container.innerHTML = `
            <p class="empty-state">
                ${UTPlay.escapeHtml(error.message)}
            </p>
        `;
    }
}

// TARJETAS DE LOS CURSOS
function renderCoursesGrid(courses) {
    const container = document.getElementById('coursesGrid');

    if (!courses.length) {
        container.innerHTML =
            '<p class="empty-state">No hay cursos registrados en este ciclo.</p>';
        return;
    }

    const safe = value =>
        UTPlay.escapeHtml(String(value ?? ''));

    container.innerHTML = courses.map(course => {
        const quizzes = Array.isArray(course.quizzes)
            ? course.quizzes
            : [];

        const quiz = quizzes[0];

        // Datos académicos
        const codigo =
            course.codigoCurso ??
            course.codigo_curso ??
            course.codigo;

        const creditos = course.creditos;

        const horas =
            course.horasSemanales ??
            course.horas_semanales ??
            course.horas;

        // Mostrar nombre completo del tipo de curso
        const tipoOriginal = String(
            course.tipoCurso ??
            course.tipo_curso ??
            ''
        ).trim();

        const tipos = {
            O: 'Obligatorio',
            E: 'Electivo'
        };

        const tipo =
            tipos[tipoOriginal.toUpperCase()] ??
            tipoOriginal;

        // Ciclo correspondiente
        const ciclo = Number(course.ciclo) === 0
            ? 'Nivelación'
            : `Ciclo ${course.ciclo}`;

        // Disponibilidad según el backend y los quizzes
        const disponible = Boolean(
            course.disponible && quiz
        );

        let estado;

        if (disponible) {
            estado = `
                <span class="cycle-tag">
                    Disponible
                </span>
            `;
        } else if (quiz) {
            estado = `
                <span class="cycle-tag lock-tag">
                    🔒 Bloqueado
                </span>
            `;
        } else {
            estado = `
                <span class="cycle-tag">
                    Próximamente
                </span>
            `;
        }

        // Acción del curso
        const accion = disponible
            ? `
                <a class="btn btn-primary"
                   href="/quiz.html?id=${encodeURIComponent(quiz.idQuiz)}">
                    Iniciar quiz →
                </a>
            `
            : `
                <button class="btn locked-button"
                        type="button"
                        disabled>
                    ${quiz ? 'Curso bloqueado' : 'Sin quiz disponible'}
                </button>
            `;

        // Créditos, horas y tipo de curso
        const datos = [
            creditos != null
                ? `${safe(creditos)} créditos`
                : '',

            horas != null
                ? `${safe(horas)} horas semanales`
                : '',

            tipo
                ? safe(tipo)
                : ''
        ].filter(Boolean).join(' · ');

        // Prerrequisitos enviados por Spring Boot
        const requisitos = Array.isArray(course.prerrequisitos)
            ? course.prerrequisitos
            : [];

        let textoPrerrequisitos =
            'Sin prerrequisitos registrados';

        if (requisitos.length > 0) {
            const nombres = requisitos.map(requisito => {
                const nombre = safe(
                    requisito.nombreCurso || 'Curso sin nombre'
                );

                const codigoRequisito = requisito.codigoCurso
                    ? `${safe(requisito.codigoCurso)}: `
                    : '';

                return `${codigoRequisito}${nombre}`;
            });

            textoPrerrequisitos = `
                Prerrequisitos:
                <strong>${nombres.join('; ')}</strong>
            `;

        } else if (course.idPrerrequisito) {
            // Compatibilidad con el formato anterior
            textoPrerrequisitos = `
                Prerrequisito:
                <strong>
                    ${safe(course.nombrePrerrequisito || 'Por confirmar')}
                </strong>
            `;
        }

        // Mensaje de bloqueo enviado por el backend
        const avisoBloqueo =
            quiz &&
            !course.disponible &&
            course.motivoBloqueo
                ? `<p>${safe(course.motivoBloqueo)}</p>`
                : '';

        // Dibujar la tarjeta
        return `
            <article class="course-card ${quiz && !course.disponible ? 'locked' : ''}">

                <div class="course-card-top">
                    <span class="cycle-tag">
                        ${safe(ciclo)}
                    </span>

                    ${estado}
                </div>

                ${codigo != null && codigo !== ''
                    ? `
                        <p>
                            <strong>Código:</strong>
                            ${safe(codigo)}
                        </p>
                    `
                    : ''
                }

                <h2>
                    ${safe(course.nombreCurso)}
                </h2>

                ${datos
                    ? `
                        <p class="course-details">
                            ${datos}
                        </p>
                    `
                    : ''
                }

                ${course.descripcion
                    ? `
                        <p>
                            ${safe(course.descripcion)}
                        </p>
                    `
                    : ''
                }

                <div class="prerequisite">
                    ${textoPrerrequisitos}
                    ${avisoBloqueo}
                </div>

                ${accion}

            </article>
        `;
    }).join('');
}

// QUIZZES
async function initQuiz() {
    const idQuiz = Number(
        new URLSearchParams(location.search).get('id')
    );

    const alert = document.getElementById('quizAlert');

    if (!Number.isInteger(idQuiz) || idQuiz < 1) {
        UTPlay.showAlert(
            alert,
            'No se indicó un quiz válido.'
        );

        document.getElementById('questionsContainer').innerHTML = '';
        return;
    }

    try {
        const quiz = await UTPlay.api(`/quiz/${idQuiz}`);

        renderQuiz(quiz);

        const form = document.getElementById('quizForm');

        form.addEventListener(
            'change',
            updateAnswerProgress
        );

        form.addEventListener(
            'submit',
            event => submitQuiz(event, quiz)
        );

    } catch (error) {
        document.getElementById('questionsContainer').innerHTML = '';
        UTPlay.showAlert(alert, error.message);
    }
}

function renderQuiz(quiz) {
    document.title = `${quiz.titulo} | UTPlay`;

    document.getElementById('quizHeader').innerHTML = `
        <div class="quiz-meta">
            <span>${UTPlay.escapeHtml(quiz.nombreCurso)}</span>
            <span>${quiz.preguntas.length} preguntas</span>
            <span>Mínimo ${quiz.puntajeMinimo}%</span>
        </div>

        <h1>${UTPlay.escapeHtml(quiz.titulo)}</h1>

        <p>${UTPlay.escapeHtml(quiz.descripcion || '')}</p>
    `;

    document.getElementById('answerProgressText').textContent =
        `0 / ${quiz.preguntas.length}`;

    document.getElementById('questionsContainer').innerHTML =
        quiz.preguntas.map((question, index) => `
            <article class="question-card">
                <span class="question-number">
                    Pregunta ${index + 1} de ${quiz.preguntas.length}
                </span>

                <h2>${UTPlay.escapeHtml(question.enunciado)}</h2>

                <div class="options-list">
                    ${Object.entries(question.alternativas)
                        .map(([letter, text]) => `
                            <label class="option-label">
                                <input type="radio"
                                       name="question-${question.idPregunta}"
                                       value="${letter}">

                                <span class="option-letter">
                                    ${letter}
                                </span>

                                <span class="option-text">
                                    ${UTPlay.escapeHtml(text)}
                                </span>
                            </label>
                        `).join('')}
                </div>
            </article>
        `).join('');

    document.getElementById('quizActions').hidden = false;
}

function updateAnswerProgress() {
    const total = document.querySelectorAll(
        '.question-card'
    ).length;

    const answered = document.querySelectorAll(
        '.question-card input:checked'
    ).length;

    document.getElementById('answerProgressText').textContent =
        `${answered} / ${total}`;

    document.getElementById('answerProgressBar').style.width =
        `${total ? answered * 100 / total : 0}%`;
}

async function submitQuiz(event, quiz) {
    event.preventDefault();

    const alert = document.getElementById('quizAlert');
    const button = document.getElementById('submitQuizButton');

    const respuestas = quiz.preguntas.map(question => ({
        idPregunta: question.idPregunta,
        alternativa: document.querySelector(
            `input[name="question-${question.idPregunta}"]:checked`
        )?.value
    }));

    if (respuestas.some(answer => !answer.alternativa)) {
        UTPlay.showAlert(
            alert,
            'Responde todas las preguntas antes de finalizar.'
        );
        return;
    }

    UTPlay.hideAlert(alert);
    UTPlay.setLoading(button, true);

    try {
        const result = await UTPlay.api('/quiz/resolver', {
            method: 'POST',
            body: JSON.stringify({
                idQuiz: quiz.idQuiz,
                respuestas
            })
        });

        sessionStorage.setItem(
            'utplay_ultimo_resultado',
            JSON.stringify(result)
        );

        const current = UTPlay.user();

        UTPlay.updateUser({
            ...current,
            puntosTotales: result.puntosTotales,
            nivel: result.nivel
        });

        location.assign('/resultado.html');

    } catch (error) {
        UTPlay.showAlert(alert, error.message);
        UTPlay.setLoading(button, false);
    }
}

// RESULTADOS
function initResultado() {
    let result = null;

    try {
        result = JSON.parse(
            sessionStorage.getItem('utplay_ultimo_resultado')
        );
    } catch (_) {
        // Sin resultado reciente
    }

    if (!result) {
        document.getElementById('resultHero').innerHTML =
            '<h1>No hay un resultado reciente</h1><p>Completa un quiz para ver aquí tu retroalimentación.</p>';

        document.getElementById('answerReview').innerHTML =
            '<p class="empty-state">Sin detalle disponible.</p>';

        return;
    }

    document.getElementById('resultHero')
        .classList.toggle('failed', !result.aprobado);

    document.getElementById('resultTitle').textContent =
        result.aprobado
            ? '¡Muy buen trabajo!'
            : '¡Sigue practicando!';

    document.getElementById('resultMessage').textContent =
        result.aprobado
            ? `Aprobaste ${result.tituloQuiz}.`
            : `Aún puedes mejorar en ${result.tituloQuiz}.`;

    document.getElementById('resultScore').textContent =
        result.puntaje;

    const badge = document.getElementById('approvalBadge');

    badge.textContent = result.aprobado
        ? 'Quiz aprobado'
        : 'Necesita refuerzo';

    badge.classList.add(
        result.aprobado ? 'good' : 'bad'
    );

    document.getElementById('correctAnswers').textContent =
        `${result.respuestasCorrectas} / ${result.totalPreguntas}`;

    document.getElementById('earnedPoints').textContent =
        `+${result.puntosGanados}`;

    document.getElementById('currentLevel').textContent =
        result.nivel;

    document.getElementById('totalPoints').textContent =
        result.puntosTotales;

    document.getElementById('retryQuiz').href =
        `/quiz.html?id=${result.idQuiz}`;

    document.getElementById('answerReview').innerHTML =
        result.detalle.map((item, index) => `
            <article class="review-item ${item.correcta ? '' : 'incorrect'}">

                <strong>
                    ${item.correcta ? '✓' : '✕'}
                    ${index + 1}.
                    ${UTPlay.escapeHtml(item.enunciado)}
                </strong>

                <p>
                    Tu respuesta:
                    ${UTPlay.escapeHtml(item.alternativaElegida)}
                </p>

                ${item.correcta
                    ? ''
                    : `
                        <p class="correct-copy">
                            Respuesta correcta:
                            ${UTPlay.escapeHtml(item.respuestaCorrecta)}
                        </p>
                    `
                }
            </article>
        `).join('');

    if (result.nuevosLogros?.length) {
        document.getElementById('newAchievements').hidden = false;

        document.getElementById('achievementList').innerHTML =
            result.nuevosLogros
                .map(achievementMarkup)
                .join('');
    }
}

// PERFIL
async function initPerfil() {
    const cached = UTPlay.user();

    try {
        const [profile, progress] = await Promise.all([
            UTPlay.api(`/usuarios/${cached.idUsuario}`),
            UTPlay.api(`/resultados/${cached.idUsuario}`)
        ]);

        UTPlay.updateUser(profile);
        fillProfile(profile, progress);

        document.getElementById('profileForm')
            .addEventListener('submit', updateProfile);

    } catch (error) {
        UTPlay.toast(error.message);
    }
}

function fillProfile(profile, progress) {
    document.getElementById('profileDisplayName').textContent =
        profile.nombre;

    document.getElementById('profileEmail').textContent =
        profile.correo;

    document.getElementById('profileLevel').textContent =
        profile.nivel;

    document.getElementById('profilePoints').textContent =
        profile.puntosTotales;

    document.getElementById('profileCycle').textContent =
        `${profile.cicloActual}.°`;

    document.getElementById('memberSince').textContent =
        UTPlay.formatDate(profile.fechaRegistro);

    document.getElementById('profileName').value =
        profile.nombre;

    document.getElementById('profileEmailInput').value =
        profile.correo;

    document.getElementById('profileCycleInput').value =
        profile.cicloActual;

    document.getElementById('profileQuizzes').textContent =
        progress.quizzesCompletados;

    document.getElementById('profileAverage').textContent =
        `${progress.promedioMejoresPuntajes}%`;

    document.getElementById('profileAchievements').textContent =
        progress.logros.length;

    document.getElementById('profileProgress').textContent =
        `${progress.porcentajeAvance}%`;

    const pointsInLevel = profile.puntosTotales % 500;

    document.getElementById('levelProgressBar').style.width =
        `${pointsInLevel / 5}%`;

    document.getElementById('nextLevelCopy').textContent =
        `${500 - pointsInLevel} puntos para el nivel ${profile.nivel + 1}`;

    renderResultsTable(
        document.getElementById('profileHistory'),
        progress.historial,
        true
    );

    document.getElementById('profileAchievementList').innerHTML =
        progress.logros.length
            ? progress.logros.map(achievementMarkup).join('')
            : '<p class="empty-state">Aún no hay logros. Completa quizzes para desbloquearlos.</p>';
}

async function updateProfile(event) {
    event.preventDefault();

    const button =
        document.getElementById('submitProfileButton') ||
        event.currentTarget.querySelector('button');

    const alert = document.getElementById('profileAlert');

    if (!event.currentTarget.reportValidity()) return;

    UTPlay.hideAlert(alert);
    UTPlay.setLoading(button, true);

    try {
        const current = UTPlay.user();

        const profile = await UTPlay.api(
            `/usuarios/${current.idUsuario}`,
            {
                method: 'PUT',
                body: JSON.stringify({
                    nombre: document.getElementById('profileName')
                        .value.trim(),

                    cicloActual: Number(
                        document.getElementById('profileCycleInput').value
                    )
                })
            }
        );

        UTPlay.updateUser(profile);

        document.getElementById('profileDisplayName').textContent =
            profile.nombre;

        document.getElementById('profileCycle').textContent =
            `${profile.cicloActual}.°`;

        UTPlay.showAlert(
            alert,
            'Perfil actualizado correctamente.',
            true
        );

    } catch (error) {
        UTPlay.showAlert(alert, error.message);
    } finally {
        UTPlay.setLoading(button, false);
    }
}

// TABLA DE RESULTADOS
function renderResultsTable(container, results, detailed) {
    if (!results.length) {
        container.innerHTML =
            '<p class="empty-state">Tu historial aparecerá después del primer quiz.</p>';
        return;
    }

    container.innerHTML = `
        <table>
            <thead>
                <tr>
                    <th>Quiz</th>
                    <th>Curso</th>
                    <th>Puntaje</th>
                    ${detailed ? '<th>Puntos</th>' : ''}
                    <th>Fecha</th>
                </tr>
            </thead>

            <tbody>
                ${results.map(item => `
                    <tr>
                        <td>
                            <strong>
                                ${UTPlay.escapeHtml(item.tituloQuiz)}
                            </strong>
                        </td>

                        <td>
                            ${UTPlay.escapeHtml(item.nombreCurso)}
                        </td>

                        <td>
                            <span class="score-pill ${item.aprobado ? 'good' : 'bad'}">
                                ${item.puntaje}%
                            </span>
                        </td>

                        ${detailed
                            ? `<td>+${item.puntosGanados}</td>`
                            : ''
                        }

                        <td>
                            ${UTPlay.formatDate(item.fecha, true)}
                        </td>
                    </tr>
                `).join('')}
            </tbody>
        </table>
    `;
}

// LOGROS
function achievementMarkup(achievement) {
    return `
        <article class="achievement-item">
            <span class="achievement-icon">◆</span>

            <div>
                <strong>
                    ${UTPlay.escapeHtml(achievement.nombre)}
                </strong>

                <span>
                    ${UTPlay.escapeHtml(achievement.descripcion)}
                </span>
            </div>
        </article>
    `;
}



/* ==========================================
   RANKING DE ESTUDIANTES - UTPLAY
========================================== */

document.addEventListener('DOMContentLoaded', () => {
    if (document.body.dataset.page !== 'ranking') return;
    if (!UTPlay.token() || !UTPlay.user()) return;

    initRanking();
});

async function initRanking() {

    const loading = document.getElementById('rankingLoading');
    const empty = document.getElementById('rankingEmpty');
    const content = document.getElementById('rankingContent');
    const errorBox = document.getElementById('rankingError');

    const cachedUser = UTPlay.user();

    loading.hidden = false;
    empty.hidden = true;
    content.hidden = true;

    UTPlay.hideAlert(errorBox);

    try {

        // Obtener clasificación y perfil actual.
        const [ranking, profile] = await Promise.all([
            UTPlay.api('/ranking'),
            UTPlay.api(`/usuarios/${cachedUser.idUsuario}`)
        ]);

        if (!Array.isArray(ranking)) {
            throw new Error(
                'El servidor no devolvió una lista de posiciones.'
            );
        }

        UTPlay.updateUser(profile);

        // Mostrar puntos personales.
        document.getElementById('rankingMyPoints').textContent =
            Number(profile.puntosTotales ?? 0).toLocaleString('es-PE');

        // Mostrar cantidad de estudiantes del top.
        document.getElementById('rankingCount').textContent =
            ranking.length;

        // Mostrar la puntuación más alta.
        document.getElementById('rankingTopPoints').textContent =
            ranking.length
                ? Number(ranking[0].puntosTotales ?? 0)
                    .toLocaleString('es-PE')
                : '0';

        // Buscar al estudiante conectado.
        const current = ranking.find(item =>
            Number(item.idUsuario) === Number(profile.idUsuario)
        );

        document.getElementById('rankingMyPosition').textContent =
            current ? `#${current.posicion}` : '—';

        document.getElementById('rankingMyMessage').textContent =
            current
                ? '¡Sigue sumando puntos y subiendo posiciones!'
                : 'Tu posición está fuera del top 10 por ahora.';

        // Ranking sin estudiantes.
        if (ranking.length === 0) {
            empty.hidden = false;
            return;
        }

        // =====================================
        // MOSTRAR PODIO: PRIMEROS 3 PUESTOS
        // =====================================

        const podium = document.getElementById('rankingPodium');

        podium.innerHTML = ranking.slice(0, 3).map(item => {

            const nombre = UTPlay.escapeHtml(item.nombre);

            const puntos = Number(
                item.puntosTotales ?? 0
            ).toLocaleString('es-PE');

            const medallas = ['🥇', '🥈', '🥉'];

            const medalla =
                medallas[Number(item.posicion) - 1] || '🏅';

            const soyYo =
                Number(item.idUsuario) === Number(profile.idUsuario);

            const inicial = UTPlay.escapeHtml(
                String(item.nombre || 'U')
                    .charAt(0)
                    .toUpperCase()
            );

            return `
                <article class="ranking-podium-card
                    ${soyYo ? 'ranking-is-me' : ''}">

                    <span class="ranking-podium-medal"
                          aria-label="Posición ${Number(item.posicion)}">
                        ${medalla}
                    </span>

                    <span class="ranking-podium-avatar"
                          aria-hidden="true">
                        ${inicial}
                    </span>

                    <strong>
                        ${nombre}
                        ${soyYo ? ' (Tú)' : ''}
                    </strong>

                    <span>
                        Nivel ${Number(item.nivel ?? 1)}
                    </span>

                    <b>${puntos} XP</b>

                </article>
            `;

        }).join('');

        // =====================================
        // MOSTRAR TABLA TOP 10
        // =====================================

        const rows = document.getElementById('rankingRows');

        rows.innerHTML = ranking.map(item => {

            const soyYo =
                Number(item.idUsuario) === Number(profile.idUsuario);

            const nombre = UTPlay.escapeHtml(item.nombre);

            const puntos = Number(
                item.puntosTotales ?? 0
            ).toLocaleString('es-PE');

            return `
                <tr class="${soyYo ? 'ranking-is-me' : ''}">

                    <td>
                        <strong>
                            #${Number(item.posicion)}
                        </strong>
                    </td>

                    <td>
                        ${nombre}
                        ${soyYo ? '<strong>(Tú)</strong>' : ''}
                    </td>

                    <td>
                        Nivel ${Number(item.nivel ?? 1)}
                    </td>

                    <td>
                        <strong>${puntos} XP</strong>
                    </td>

                </tr>
            `;

        }).join('');

        // Mostrar clasificación.
        content.hidden = false;

    } catch (error) {

        UTPlay.showAlert(errorBox, error.message);

    } finally {

        loading.hidden = true;

    }
}

