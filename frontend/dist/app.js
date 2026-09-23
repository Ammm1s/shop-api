import { initAdmin } from './admin.js';
const $ = (selector) => document.querySelector(selector);
const icons = {
  user: '<circle cx="12" cy="8" r="3.5"/><path d="M5 21v-2a7 7 0 0 1 14 0v2"/>',
  bag: '<path d="M5 7h14l1 14H4L5 7Z"/><path d="M9 8V6a3 3 0 0 1 6 0v2"/>',
  arrow: '<path d="M4 12h15M13 5l7 7-7 7"/>',
  search: '<circle cx="10.5" cy="10.5" r="6.5"/><path d="m16 16 5 5"/>',
  sort: '<path d="M4 6h16M4 12h11M4 18h6"/>',
  close: '<path d="m6 6 12 12M6 18 18 6"/>',
  plus: '<path d="M12 5v14M5 12h14"/>', minus: '<path d="M5 12h14"/>',
  check: '<path d="m5 12 4 4L19 6"/>',
  box: '<path d="m12 3 9 5v9l-9 5-9-5V8l9-5ZM3 8l9 5 9-5M12 13v9M7.5 5.5l9 5v5"/>',
};
const icon = (name) => `<svg viewBox="0 0 24 24" aria-hidden="true">${icons[name] || icons.box}</svg>`;
function fillIcons(root = document) { root.querySelectorAll('[data-icon]').forEach(el => el.innerHTML = icon(el.dataset.icon)); }
const esc = (value) => String(value ?? '').replace(/[&<>"']/g, c => ({'&':'&amp;', '<':'&lt;', '>':'&gt;', '"':'&quot;', "'":'&#39;'}[c]));
const money = (value) => new Intl.NumberFormat('ru-RU', {style:'currency', currency:'RUB', maximumFractionDigits:2}).format(Number(value));
const demo = [
  {id:900001, name:'Беспроводные наушники', description:'Личный саундтрек. Дома, в городе и в дороге.', price:12990, stock:12, category:{id:1,name:'Наушники'}, image:'/assets/hero.jpg', tag:'ДЛЯ ТВОЕГО РИТМА'},
  {id:900002, name:'Механическая клавиатура', description:'Тактильный отклик. Больше удовольствия от каждого нажатия.', price:8490, stock:8, category:{id:2,name:'Для работы'}, image:'/assets/keyboard.jpg', tag:'РАБОЧЕЕ НАСТРОЕНИЕ'},
  {id:900003, name:'Портативная колонка', description:'Компактный формат для любимой музыки.', price:5990, stock:0, category:{id:3,name:'Акустика'}, image:'/assets/speaker.jpg', tag:'ЗВУК РЯДОМ'},
];
function storageGet(storage, key, fallback) { try { return JSON.parse(storage.getItem(key)) ?? fallback; } catch { return fallback; } }
function storageSet(storage, key, data) { try { storage.setItem(key, JSON.stringify(data)); } catch {} }
let session = storageGet(sessionStorage, 'shop-session', null);
if (session && (typeof session.token !== 'string' || !(session.expiresAt > Date.now()))) {session=null;storageSet(sessionStorage,'shop-session',null);}
const state = { products:[], categories:[], mode:'loading', cart:[], category:'all', user:null, authMode:'login', busy:false, orders:[], ordersPage:0, orderTotalPages:0, orderRequest:0, cancelId:null };
let toastTimer;
function toast(text) { $('#toast').textContent=text;$('#toast').classList.add('visible');clearTimeout(toastTimer);toastTimer=setTimeout(()=>$('#toast').classList.remove('visible'),3500); }
function openDialog(id) { const dlg=$(`#${id}`); if(!dlg.open) dlg.showModal(); }
function empty(title,text,action='') {return `<div class="empty-state">${icon('box')}<h3>${esc(title)}</h3><p>${esc(text)}</p>${action}</div>`;}
function clearSession() {session=null;state.user=null;storageSet(sessionStorage,'shop-session',null);renderAccount();}
async function api(path, options={}) {
  const {auth=false,...settings}=options;
  if (auth && (!session || session.expiresAt <= Date.now())) {
    clearSession();throw Object.assign(new Error('Войди в аккаунт, чтобы продолжить.'),{status:401});
  }
  const headers={'Accept':'application/json',...settings.headers};
  if(settings.body) headers['Content-Type']='application/json';
  if(auth) headers.Authorization=`Bearer ${session.token}`;
  let response;
  try {response=await fetch('/api'+path,{...settings,headers,signal:AbortSignal.timeout(15000)});}
  catch {throw Object.assign(new Error('Не удалось подключиться. Попробуй ещё раз.'),{status:503});}
  const text=await response.text();
  let data;try{data=text?JSON.parse(text):null;}catch{throw new Error('Не удалось прочитать ответ магазина.');}
  if(!response.ok){
    if(response.status===401 && auth) clearSession();
    const defaults={401:'Сессия закончилась. Войди ещё раз.',403:'Недостаточно прав.',409:'Данные изменились. Обнови страницу и попробуй снова.',503:'Магазин временно недоступен.'};
    const details=data?.errors && typeof data.errors==='object'?Object.values(data.errors).join('\n'):'';
    throw Object.assign(new Error(details || data?.message || defaults[response.status] || 'Не удалось выполнить запрос.'),{status:response.status});
  }
  return data;
}
function pageInfo(data){return {items:Array.isArray(data)?data:(data?.content||[]),totalPages:data?.page?.totalPages??data?.totalPages??1};}
async function loadCatalog(){
  const retry=$('#retry-api');if(retry) retry.disabled=true;
  try{
    const [first,categories]=await Promise.all([api('/products?size=100&sort=id,asc'),api('/categories')]);
    const info=pageInfo(first);const products=[...info.items];
    for(let page=1;page<info.totalPages;page++) products.push(...pageInfo(await api(`/products?size=100&sort=id,asc&page=${page}`)).items);
    state.products=products;state.categories=Array.isArray(categories)?categories:[];state.mode='live';
    $('#connection-notice').hidden=true;
  }catch{
    state.products=demo;state.categories=demo.map(p=>p.category);state.mode='demo';
    $('#connection-notice').hidden=false;
    $('#connection-notice').innerHTML='<span><strong>Демо-каталог.</strong> Магазин пока недоступен. Товары и цены приведены для примера; оформление отключено.</span><button id="retry-api">Подключиться снова</button>';
  }
  const saved=storageGet(localStorage,`shop-cart-${state.mode}`,[]);
  state.cart=(Array.isArray(saved)?saved:[]).filter(i=>Number.isInteger(i.id)&&Number.isInteger(i.quantity)&&i.quantity>0).map(i=>{
    const p=state.products.find(p=>p.id===i.id);return p?{id:i.id,quantity:Math.min(i.quantity,p.stock)}:null;
  }).filter(i=>i&&i.quantity>0);
  renderCategories();renderProducts();renderCart();admin.render();
}
function renderCategories(){
  const cats=[{id:'all',name:'Всё'},...state.categories];
  if(!cats.some(c=>String(c.id)===state.category)) state.category='all';
  $('#categories').innerHTML=cats.map(c=>`<button class="category ${String(c.id)===state.category?'active':''}" data-category="${esc(c.id)}" aria-pressed="${String(c.id)===state.category}">${esc(c.name)}</button>`).join('');
}
function productPicture(p,className=''){
  let source='';
  if(p.imageUrl){
    try{
      const url=new URL(p.imageUrl);
      if(url.protocol==='https:'&&url.hostname&&!url.username&&!url.password)source=url.href;
    }catch{}
  }else if(state.mode==='demo'&&/^\/assets\/[\w-]+\.jpg$/.test(p.image||''))source=p.image;
  return source?`<img data-product-image class="${className}" src="${esc(source)}" alt="${esc(p.name)}" loading="lazy" referrerpolicy="no-referrer">`:`<div class="product-placeholder ${className}" role="img" aria-label="Изображение отсутствует">${icon('box')}</div>`;
}
document.addEventListener('error',event=>{
  const img=event.target;
  if(!(img instanceof HTMLImageElement)||!img.hasAttribute('data-product-image'))return;
  const placeholder=document.createElement('div');
  placeholder.className=`product-placeholder ${img.className}`;
  placeholder.setAttribute('role','img');
  placeholder.setAttribute('aria-label','Изображение недоступно');
  placeholder.innerHTML=icon('box');
  img.replaceWith(placeholder);
},true);
function renderProducts(){
  const query=$('#search').value.trim().toLocaleLowerCase('ru');
  const stockOnly=$('#in-stock').checked;
  let products=state.products.filter(p=>(state.category==='all'||String(p.category?.id)===state.category)&&(!stockOnly||p.stock>0)&&(`${p.name} ${p.description??''}`.toLocaleLowerCase('ru').includes(query)));
  const sort=$('#sort').value;
  if(sort==='price-asc')products.sort((a,b)=>a.price-b.price);
  if(sort==='price-desc')products.sort((a,b)=>b.price-a.price);
  if(sort==='name')products.sort((a,b)=>a.name.localeCompare(b.name,'ru'));
  $('#product-total').textContent=state.products.length;
  $('#result-count').textContent=`Найдено: ${products.length}`;
  $('#reset-filters').hidden=!query&&!stockOnly&&state.category==='all'&&sort==='default';
  $('#product-grid').innerHTML=products.length?products.map(p=>`<article class="product-card"><button class="product-image" data-detail="${p.id}" aria-label="Подробнее: ${esc(p.name)}">${productPicture(p)}${p.tag?`<span class="tag">${esc(p.tag)}</span>`:''}<span class="image-arrow">${icon('arrow')}</span></button><div class="product-info"><div class="product-category">${esc(p.category?.name||'Каталог')}</div><button class="product-title" data-detail="${p.id}">${esc(p.name)}</button><p class="product-description">${esc(p.description||'Подробнее о товаре — в карточке.')}</p><div class="product-bottom"><div class="price">${money(p.price)}<span class="stock ${p.stock>0?'':'out'}">${p.stock>0?'В наличии':'Нет в наличии'}</span></div><button class="add-button" data-add="${p.id}" ${p.stock>0?'':'disabled'} aria-label="Добавить в корзину: ${esc(p.name)}">${icon('plus')}</button></div></div></article>`).join(''):empty('Ничего не нашлось',state.products.length?'Попробуй другое название или выбери другую категорию.':'Здесь появятся товары, добавленные в магазин.');
}
function saveCart(){storageSet(localStorage,`shop-cart-${state.mode}`,state.cart);renderCart();}
function addToCart(id){
  const p=state.products.find(p=>p.id===id);if(!p||p.stock<1)return;
  const item=state.cart.find(i=>i.id===id);
  if(item && item.quantity>=p.stock){toast('В корзине уже всё доступное количество');return;}
  if(item)item.quantity++;else state.cart.push({id,quantity:1});
  saveCart();toast('Добавлено в корзину');
}
function renderCart(){
  const count=state.cart.reduce((sum,i)=>sum+i.quantity,0);
  $('#cart-count').textContent=count;$('#drawer-count').textContent=count||'';
  $('#cart-button').setAttribute('aria-label',`Корзина, товаров: ${count}`);
  const total=state.cart.reduce((sum,i)=>sum+(state.products.find(p=>p.id===i.id)?.price||0)*i.quantity,0);
  $('#cart-items').innerHTML=state.cart.length?state.cart.map(i=>{
    const p=state.products.find(p=>p.id===i.id);if(!p)return '';
    return `<div class="cart-item">${productPicture(p,'cart-thumb')}<div class="cart-item-info"><h3>${esc(p.name)}</h3><button class="icon-button remove" data-remove="${p.id}" aria-label="Удалить ${esc(p.name)}">${icon('close')}</button><div class="cart-item-price">${money(p.price*i.quantity)}</div><div class="quantity"><button data-quantity="${p.id}" data-delta="-1" aria-label="Уменьшить количество">${icon('minus')}</button><span>${i.quantity}</span><button data-quantity="${p.id}" data-delta="1" ${i.quantity>=p.stock?'disabled':''} aria-label="Увеличить количество">${icon('plus')}</button></div></div></div>`;
  }).join(''):empty('Здесь пока пусто','Добавь вещи, которые тебе понравились.','<button class="secondary-button" data-close="cart-dialog">Продолжить выбор</button>');
  $('#cart-summary').innerHTML=state.cart.length?`<div class="total"><span>Итого</span><strong>${money(total)}</strong></div><button id="checkout" class="primary-button full" ${state.mode!=='live'||state.busy?'disabled':''}>${state.busy?'Оформляем…':session?'Оформить заказ':'Войти и оформить'} ${icon('arrow')}</button><p>${state.mode==='demo'?'Это демо-корзина. Заказы станут доступны после подключения магазина.':'Заказ будет создан без онлайн-оплаты. Итоговая цена и остатки проверяются при оформлении.'}</p><p id="checkout-error" class="form-error" role="alert"></p>`:'';
}
function showProduct(id){
  const p=state.products.find(p=>p.id===id);if(!p)return;
  $('#product-detail').innerHTML=`${productPicture(p,'detail-image')}<div class="detail-content"><div class="eyebrow">${esc(p.category?.name||'Каталог')}</div><h2>${esc(p.name)}</h2><p>${esc(p.description||'Описание ещё не добавлено.')}</p><div class="product-bottom"><div class="price">${money(p.price)}<span class="stock ${p.stock>0?'':'out'}">${p.stock>0?`В наличии: ${p.stock} шт.`:'Нет в наличии'}</span></div><button class="primary-button" data-add="${p.id}" ${p.stock>0?'':'disabled'}>В корзину ${icon('plus')}</button></div>${state.mode==='demo'?'<p>Демонстрационный товар. Недоступен для заказа.</p>':''}</div>`;
  openDialog('product-dialog');
}
function renderAccount(){
  const canManage=['ADMIN','OWNER'].includes(state.user?.role);
  const roleLabels={USER:'Покупатель',ADMIN:'Администратор',OWNER:'Владелец'};
  $('#account-label').textContent=state.user?.name||'Войти';
  $('#admin-nav').hidden=!canManage;admin.render();
  $('#profile-content').innerHTML=state.user?`<div class="profile-info"><p><span>Имя</span>${esc(state.user.name)}</p><p><span>Email</span>${esc(state.user.email)}</p><p><span>Роль</span>${esc(roleLabels[state.user.role]||state.user.role)}</p></div><a href="#orders" class="primary-button full" data-close="profile-dialog">Мои заказы ${icon('arrow')}</a>${canManage?'<a href="#admin" class="secondary-button full" data-close="profile-dialog">Панель администратора</a>':''}`:'';
}
function showAuth(){
  if(state.mode==='demo'){toast('Вход будет доступен после подключения магазина');return;}
  $('#auth-error').textContent='';openDialog('auth-dialog');
}
function setAuthMode(mode){
  state.authMode=mode;const register=mode==='register';
  $('#auth-title').textContent=register?'Привет, это Shop.':'С возвращением.';
  $('#auth-subtitle').textContent=register?'Создай аккаунт для своих покупок.':'Войди, чтобы оформить заказ.';
  $('#name-field').hidden=!register;$('#auth-name').required=register;
  $('#auth-password').autocomplete=register?'new-password':'current-password';
  if(register)$('#auth-password').minLength=8;else $('#auth-password').removeAttribute('minlength');
  $('#auth-submit').innerHTML=(register?'Создать аккаунт':'Войти')+' '+icon('arrow');
  $('#auth-switch-label').textContent=register?'Уже есть аккаунт?':'Пока нет аккаунта?';$('#auth-switch').textContent=register?'Войти':'Создать';$('#auth-error').textContent='';
}
$('#auth-form').addEventListener('submit',async event=>{
  event.preventDefault();const form=event.currentTarget;const values=new FormData(form);const submit=$('#auth-submit');
  if(submit.disabled)return;submit.disabled=true;$('#auth-switch').disabled=true;$('#auth-error').textContent='';
  const email=String(values.get('email')).trim();const password=String(values.get('password'));let registered=false;
  try{
    if(state.authMode==='register'){await api('/users',{method:'POST',body:JSON.stringify({name:String(values.get('name')).trim(),email,password})});registered=true;}
    const result=await api('/auth/login',{method:'POST',body:JSON.stringify({email,password})});
    if(!result?.token||!Number.isFinite(result.expiresIn))throw new Error('Не удалось получить сессию. Попробуй войти ещё раз.');
    session={token:result.token,expiresAt:Date.now()+result.expiresIn*1000};storageSet(sessionStorage,'shop-session',session);
    state.user=await api('/users/me',{auth:true});renderAccount();form.reset();$('#auth-dialog').close();toast(registered?'Аккаунт создан':'Ты вошёл в аккаунт');
    renderCart();if(location.hash==='#orders')loadOrders();
  }catch(error){
    if(registered){setAuthMode('login');$('#auth-error').textContent='Аккаунт создан. Повтори вход: '+error.message;}
    else $('#auth-error').textContent=error.message;
  }finally{submit.disabled=false;$('#auth-switch').disabled=false;}
});
async function checkout(){
  if(state.busy||state.mode!=='live'||!state.cart.length)return;
  if(!session){$('#cart-dialog').close();showAuth();return;}
  state.busy=true;renderCart();
  try{
    const order=await api('/orders',{auth:true,method:'POST',body:JSON.stringify({items:state.cart.map(i=>({productId:i.id,quantity:i.quantity}))})});
    state.cart=[];saveCart();$('#cart-dialog').close();toast(`Заказ №${order.id} создан`);
    location.hash='orders';await loadCatalog();
  }catch(error){
    if(error.status===401){$('#cart-dialog').close();showAuth();}
    else {state.busy=false;renderCart();$('#checkout-error').textContent=error.message;}
  }finally{state.busy=false;const button=$('#checkout');if(button){button.disabled=state.mode!=='live';button.innerHTML=(session?'Оформить заказ':'Войти и оформить')+' '+icon('arrow');}}
}
async function loadOrders(page=0){
  const requestId=++state.orderRequest;
  if(!session){$('#orders-content').innerHTML=empty('Твои заказы будут здесь','Войди в аккаунт, чтобы посмотреть покупки.','<button class="primary-button" data-login>Войти в аккаунт</button>');return;}
  $('#orders-content').innerHTML='<div class="loading-card">Загружаем заказы…</div>';
  try{
    const data=await api(`/orders?page=${page}&size=10&sort=createdAt,desc`,{auth:true});
    if(requestId!==state.orderRequest)return;
    const info=pageInfo(data);state.orders=info.items;state.ordersPage=page;state.orderTotalPages=info.totalPages;
    renderOrders();
  }catch(error){if(requestId===state.orderRequest)$('#orders-content').innerHTML=empty('Не удалось загрузить заказы',error.message,error.status===401?'<button class="primary-button" data-login>Войти</button>':'<button class="secondary-button" id="retry-orders">Повторить</button>');}
}
function renderOrders(){
  const labels={NEW:'Новый',PAID:'Оплачен',SHIPPED:'Отправлен',CANCELLED:'Отменён'};
  $('#orders-content').innerHTML=state.orders.length?state.orders.map(o=>`<article class="order-card"><div class="order-head"><div><h3>Заказ №${o.id}</h3><small>${o.createdAt?new Date(o.createdAt).toLocaleString('ru-RU',{dateStyle:'long',timeStyle:'short'}):''}</small></div><span class="status ${esc(o.status)}">${esc(labels[o.status]||o.status)}</span></div><div class="order-lines">${(o.items||[]).map(i=>`<div class="order-line"><span>${esc(state.products.find(p=>p.id===i.productId)?.name||`Товар №${i.productId}`)} × ${i.quantity}</span><span>${money(i.price*i.quantity)}</span></div>`).join('')}</div><div class="order-foot"><strong>${money(o.totalPrice)}</strong>${o.status==='NEW'?`<button class="text-button" data-cancel="${o.id}">Отменить заказ</button>`:''}</div></article>`).join('')+`<div class="load-more"><button class="secondary-button" data-page="${state.ordersPage-1}" ${state.ordersPage===0?'disabled':''}>Назад</button><span style="padding:12px 18px">${state.ordersPage+1} / ${state.orderTotalPages}</span><button class="secondary-button" data-page="${state.ordersPage+1}" ${state.ordersPage+1>=state.orderTotalPages?'disabled':''}>Далее</button></div>`:empty('Пока ни одного заказа','Самое время найти что-нибудь для себя.','<a class="primary-button" href="#catalog">Открыть каталог</a>');
}
async function cancelOrder(){
  const button=$('#confirm-cancel');if(button.disabled)return;button.disabled=true;
  try{await api(`/orders/${state.cancelId}/cancel`,{auth:true,method:'POST'});$('#confirm-dialog').close();toast('Заказ отменён');await Promise.all([loadCatalog(),loadOrders(state.ordersPage)]);}
  catch(error){$('#confirm-dialog').close();toast(error.message);if(error.status===401)showAuth();}
  finally{button.disabled=false;}
}
function route(){
  const orders=location.hash==='#orders', managing=location.hash==='#admin';
  $('#catalog-view').hidden=orders||managing;$('#orders-view').hidden=!orders;$('#admin-view').hidden=!managing;
  if(managing){admin.render();window.scrollTo(0,0);}
  document.querySelectorAll('[data-nav]').forEach(el=>{el.classList.toggle('active',el.dataset.nav===(orders?'orders':managing?'admin':'catalog'));if(el.classList.contains('active'))el.setAttribute('aria-current','page');else el.removeAttribute('aria-current');});
  if(orders){loadOrders();window.scrollTo(0,0);}else if(location.hash==='#catalog')window.scrollTo(0,0);
}
document.addEventListener('click',event=>{
  const button=event.target.closest('button,a');if(!button||button.disabled)return;
  if(button.dataset.close)$(`#${button.dataset.close}`).close();
  if(button.dataset.add){addToCart(Number(button.dataset.add));if($('#product-dialog').open){button.innerHTML='Добавлено '+icon('check');setTimeout(()=>{if(button.isConnected)button.innerHTML='В корзину '+icon('plus');},1300);}}
  if(button.dataset.detail)showProduct(Number(button.dataset.detail));
  if(button.dataset.category){state.category=button.dataset.category;renderCategories();renderProducts();}
  if(button.dataset.remove){state.cart=state.cart.filter(i=>i.id!==Number(button.dataset.remove));saveCart();}
  if(button.dataset.quantity){const i=state.cart.find(i=>i.id===Number(button.dataset.quantity));if(i){const p=state.products.find(p=>p.id===i.id);i.quantity=Math.max(0,Math.min(p.stock,i.quantity+Number(button.dataset.delta)));state.cart=state.cart.filter(i=>i.quantity>0);saveCart();}}
  if(button.hasAttribute('data-login'))showAuth();
  if(button.dataset.cancel){state.cancelId=Number(button.dataset.cancel);openDialog('confirm-dialog');}
  if(button.dataset.page!==undefined)loadOrders(Number(button.dataset.page));
  if(button.id==='retry-api')loadCatalog();
  if(button.id==='retry-orders')loadOrders();
  if(button.id==='checkout')checkout();
});
$('#cart-button').addEventListener('click',()=>{renderCart();openDialog('cart-dialog');});
$('#account-button').addEventListener('click',()=>state.user?openDialog('profile-dialog'):showAuth());
$('#auth-switch').addEventListener('click',()=>setAuthMode(state.authMode==='login'?'register':'login'));
$('#confirm-cancel').addEventListener('click',cancelOrder);
$('#logout').addEventListener('click',()=>{clearSession();state.orders=[];state.cart=[];saveCart();$('#profile-dialog').close();route();toast('Ты вышел из аккаунта');});
$('#search').addEventListener('input',renderProducts);$('#sort').addEventListener('change',renderProducts);$('#in-stock').addEventListener('change',renderProducts);
$('#reset-filters').addEventListener('click',()=>{$('#search').value='';$('#sort').value='default';$('#in-stock').checked=false;state.category='all';renderCategories();renderProducts();});
document.addEventListener('keydown',e=>{if(e.key==='/'&&!['INPUT','TEXTAREA','SELECT'].includes(document.activeElement.tagName)&&!document.querySelector('dialog[open]')){e.preventDefault();location.hash='products';$('#search').focus();}});
document.querySelectorAll('dialog').forEach(dialog=>{dialog.addEventListener('click',event=>{if(event.target===dialog){const r=dialog.getBoundingClientRect();if(event.clientX<r.left||event.clientX>r.right||event.clientY<r.top||event.clientY>r.bottom)dialog.close();}});});
window.addEventListener('hashchange',route);
const admin=initAdmin({state,api,loadCatalog,esc,money,icon,empty,toast,openDialog});
fillIcons();$('#year').textContent=new Date().getFullYear();
await loadCatalog();
if(session){try{state.user=await api('/users/me',{auth:true});renderAccount();}catch(error){if(error.status===401)clearSession();}}
route();
