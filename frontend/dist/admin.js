export function initAdmin({state,api,loadCatalog,esc,money,icon,empty,toast,openDialog}) {
  const $=selector=>document.querySelector(selector);
  let tab='products';
  let editingId=null;
  let deletingId=null;
  let pendingRole=null;
  let saving=false;
  let refreshing=false;
  let users=[];
  let usersLoading=false;
  let usersError='';
  let orders=[];
  let ordersPage=0;
  let ordersTotalPages=0;
  let ordersLoading=false;
  let ordersError='';
  let ordersRequest=0;
  let pendingOrder=null;

  const authorized=()=>['ADMIN','OWNER'].includes(state.user?.role);
  const isOwner=()=>state.user?.role==='OWNER';

  function requireAccess(){
    if(!authorized())throw new Error('Для этого действия войди в аккаунт администратора.');
    if(state.mode!=='live')throw new Error('Магазин недоступен. Обнови данные перед изменением каталога.');
  }

  function requireOwner(){
    requireAccess();
    if(!isOwner())throw new Error('Назначать администраторов может только владелец.');
  }

  function render(){
    const allowed=authorized()&&state.mode==='live';
    if(tab==='users'&&!isOwner())tab='products';
    $('#admin-workspace').hidden=!allowed;
    $('#admin-access').hidden=allowed;
    $('#admin-users-tab').hidden=!isOwner();

    if(!allowed){
      if(!state.user){
        $('#admin-access').innerHTML=empty('Вход для администратора','Войди в аккаунт с ролью администратора или владельца.','<button class="primary-button" data-login>Войти в аккаунт</button>');
      }else if(!authorized()){
        $('#admin-access').innerHTML=empty('Недостаточно прав','Управление магазином доступно администратору и владельцу.','<a href="#catalog" class="secondary-button">Вернуться в каталог</a>');
      }else{
        $('#admin-access').innerHTML=empty('Магазин недоступен','Не удалось загрузить данные. Проверь, что магазин запущен, и повтори попытку.','<button class="secondary-button" data-admin-retry>Подключиться снова</button>');
      }
      return;
    }

    const fourthStat=isOwner()&&users.length
      ?['Пользователей',users.length]
      :['Нет в наличии',state.products.filter(product=>product.stock===0).length];
    const stats=[
      ['Товаров в каталоге',state.products.length],
      ['Категорий',state.categories.length],
      ['Единиц на складе',state.products.reduce((total,product)=>total+Number(product.stock),0)],
      fourthStat,
    ];
    $('#admin-stats').innerHTML=stats.map(([label,value])=>`<div class="admin-stat"><span>${label}</span><strong>${Number(value).toLocaleString('ru-RU')}</strong></div>`).join('');

    document.querySelectorAll('[data-admin-tab]').forEach(element=>{
      element.setAttribute('aria-pressed',String(element.dataset.adminTab===tab));
    });
    $('#admin-create').hidden=tab==='users'||tab==='orders';
    if(tab==='products'||tab==='categories'){
      $('#admin-create').innerHTML=icon('plus')+(tab==='products'?' Добавить товар':' Новая категория');
    }
    $('#admin-search').hidden=tab==='orders';
    $('#admin-search').placeholder=tab==='users'?'Поиск по имени или email':'Поиск по названию';

    if(tab==='users')renderUsers();
    else if(tab==='orders')renderOrders();
    else renderCatalogSection();
  }

  function renderCatalogSection(){
    const query=$('#admin-search').value.trim().toLocaleLowerCase('ru');
    const source=tab==='products'?state.products:state.categories;
    const items=source.filter(item=>item.name.toLocaleLowerCase('ru').includes(query));
    if(!items.length){
      const title=query?'Ничего не найдено':tab==='products'?'Добавь первый товар':'Создай первую категорию';
      const message=query?'Попробуй другое название.':tab==='products'
        ?'Начни с категории, затем добавь название, цену и остаток товара.'
        :'Категории помогают покупателям находить нужные товары.';
      $('#admin-list').innerHTML=empty(title,message);
      return;
    }
    if(tab==='categories'){
      $('#admin-list').innerHTML=`<div class="admin-category-grid">${items.map(category=>`<article class="admin-category-card"><div class="admin-category-icon">${icon('box')}</div><h3>${esc(category.name)}</h3><p>${esc(category.description||'Описание не добавлено.')}</p><span class="muted">Товаров: ${state.products.filter(product=>product.category?.id===category.id).length}</span></article>`).join('')}</div>`;
      return;
    }
    $('#admin-list').innerHTML=`<div class="admin-table-wrap" tabindex="0" role="region" aria-label="Таблица товаров"><table class="admin-table"><thead><tr><th scope="col">Товар</th><th scope="col">Категория</th><th scope="col">Цена</th><th scope="col">Остаток</th><th scope="col">Действия</th></tr></thead><tbody>${items.map(product=>`<tr><td><strong>${esc(product.name)}</strong><small>№ ${product.id}</small></td><td>${esc(product.category?.name||'Без категории')}</td><td class="admin-price">${money(product.price)}</td><td><span class="admin-stock ${product.stock===0?'empty-stock':''}">${product.stock} шт.</span></td><td><div class="admin-row-actions"><button class="text-button" data-admin-edit="${product.id}" aria-label="Изменить ${esc(product.name)}">Изменить</button><button class="text-button danger-text" data-admin-delete="${product.id}" aria-label="Удалить ${esc(product.name)}">Удалить</button></div></td></tr>`).join('')}</tbody></table></div>`;
  }

  function renderUsers(){
    if(usersLoading){
      $('#admin-list').innerHTML='<div class="loading-card">Загружаем пользователей…</div>';
      return;
    }
    if(usersError){
      $('#admin-list').innerHTML=empty('Не удалось загрузить пользователей',usersError,'<button class="secondary-button" data-users-retry>Повторить</button>');
      return;
    }
    const query=$('#admin-search').value.trim().toLocaleLowerCase('ru');
    const filtered=users.filter(user=>`${user.name} ${user.email} ${user.role}`.toLocaleLowerCase('ru').includes(query));
    if(!filtered.length){
      $('#admin-list').innerHTML=empty(query?'Ничего не найдено':'Пользователей пока нет',query?'Попробуй другое имя или email.':'Новые покупатели появятся здесь после регистрации.');
      return;
    }
    const labels={USER:'Покупатель',ADMIN:'Администратор',OWNER:'Владелец'};
    $('#admin-list').innerHTML=`<div class="admin-table-wrap" tabindex="0" role="region" aria-label="Таблица пользователей"><table class="admin-table users-table"><thead><tr><th scope="col">Пользователь</th><th scope="col">Email</th><th scope="col">Роль</th><th scope="col">Действие</th></tr></thead><tbody>${filtered.map(user=>{
      const isCurrent=user.userId===state.user?.userId;
      const action=user.role==='OWNER'
        ?`<span class="owner-note">${isCurrent?'Это вы':'Защищённая роль'}</span>`
        :`<button class="text-button ${user.role==='ADMIN'?'danger-text':''}" data-admin-role="${user.userId}">${user.role==='ADMIN'?'Снять роль':'Назначить администратором'}</button>`;
      return `<tr><td><strong>${esc(user.name)}</strong><small>№ ${user.userId}</small></td><td>${esc(user.email)}</td><td><span class="role-badge role-${String(user.role).toLowerCase()}">${esc(labels[user.role]||user.role)}</span></td><td>${action}</td></tr>`;
    }).join('')}</tbody></table></div>`;
  }

  async function loadUsers(){
    if(!isOwner()||usersLoading)return;
    usersLoading=true;usersError='';render();
    try{
      const first=await api('/users?size=100&sort=userId,asc',{auth:true});
      const items=Array.isArray(first)?[...first]:[...(first?.content||[])];
      const totalPages=first?.page?.totalPages??first?.totalPages??1;
      for(let page=1;page<totalPages;page++){
        const data=await api(`/users?size=100&sort=userId,asc&page=${page}`,{auth:true});
        items.push(...(Array.isArray(data)?data:(data?.content||[])));
      }
      users=items;
    }catch(error){
      usersError=error.message;
    }finally{
      usersLoading=false;render();
    }
  }

  function renderOrders(){
    if(ordersLoading){
      $('#admin-list').innerHTML='<div class="loading-card">Загружаем заказы…</div>';
      return;
    }
    if(ordersError){
      $('#admin-list').innerHTML=empty('Не удалось загрузить заказы',ordersError,'<button class="secondary-button" data-orders-retry>Повторить</button>');
      return;
    }
    if(!orders.length){
      $('#admin-list').innerHTML=empty('Заказов пока нет','Новые заказы покупателей появятся здесь.');
      return;
    }
    const labels={NEW:'Новый',PAID:'Оплачен',SHIPPED:'Отправлен',CANCELLED:'Отменён'};
    const rows=orders.map(order=>{
      const next=order.status==='NEW'?'PAID':order.status==='PAID'?'SHIPPED':null;
      const action=next?`<button class="text-button" data-admin-order-status="${esc(order.id)}">${next==='PAID'?'Отметить оплату':'Отметить отправку'}</button>`:'<span class="muted">Нет действий</span>';
      const date=order.createdAt?new Date(order.createdAt).toLocaleString('ru-RU',{dateStyle:'medium',timeStyle:'short'}):'Дата неизвестна';
      const items=(order.items||[]).map(item=>{
        const product=state.products.find(product=>product.id===item.productId);
        return `${esc(product?.name||`Товар №${item.productId}`)} × ${esc(item.quantity)}`;
      }).join(', ')||'Товары не указаны';
      return `<tr><td><strong>Заказ №${esc(order.id)}</strong><small>${esc(date)}</small></td><td><span class="status ${esc(order.status)}">${esc(labels[order.status]||order.status)}</span></td><td class="admin-order-items">${items}</td><td class="admin-price">${money(order.totalPrice)}</td><td>${action}</td></tr>`;
    }).join('');
    const pages=ordersTotalPages>1?`<div class="admin-pages"><button class="secondary-button" data-orders-page="${ordersPage-1}" ${ordersPage===0?'disabled':''}>Назад</button><span>Страница ${ordersPage+1} из ${ordersTotalPages}</span><button class="secondary-button" data-orders-page="${ordersPage+1}" ${ordersPage+1>=ordersTotalPages?'disabled':''}>Далее</button></div>`:'';
    $('#admin-list').innerHTML=`<div class="admin-table-wrap" tabindex="0" role="region" aria-label="Таблица заказов"><table class="admin-table admin-orders-table"><thead><tr><th scope="col">Заказ</th><th scope="col">Статус</th><th scope="col">Товары</th><th scope="col">Сумма</th><th scope="col">Действие</th></tr></thead><tbody>${rows}</tbody></table></div>${pages}`;
  }

  async function loadOrders(page=0){
    if(!authorized()||state.mode!=='live')return;
    const requestId=++ordersRequest;
    ordersLoading=true;ordersError='';render();
    try{
      const data=await api(`/orders/admin?page=${page}&size=10&sort=createdAt,desc`,{auth:true});
      if(requestId!==ordersRequest)return;
      orders=Array.isArray(data)?data:(data?.content||[]);
      ordersPage=page;
      ordersTotalPages=data?.page?.totalPages??data?.totalPages??1;
    }catch(error){
      if(requestId!==ordersRequest)return;
      ordersError=[400,404].includes(error.status)?'Список заказов ещё не подключён на сервере.':error.message;
    }finally{
      if(requestId===ordersRequest){ordersLoading=false;render();}
    }
  }

  function showOrderStatus(id){
    requireAccess();if(saving)return;
    const order=orders.find(item=>item.id===id);
    if(!order)return;
    const status=order.status==='NEW'?'PAID':order.status==='PAID'?'SHIPPED':null;
    if(!status)return;
    pendingOrder={id,status};
    $('#admin-order-title').textContent=status==='PAID'?'Отметить заказ оплаченным?':'Отметить заказ отправленным?';
    $('#admin-order-description').textContent=`Заказ №${id} перейдёт в статус «${status==='PAID'?'Оплачен':'Отправлен'}».`;
    $('#admin-order-hint').textContent=status==='PAID'?'Убедись, что оплата получена. Эта кнопка только меняет статус заказа и не принимает платёж.':'Подтверждай отправку после передачи заказа в доставку.';
    $('#admin-order-submit').textContent=status==='PAID'?'Подтвердить оплату':'Подтвердить отправку';
    $('#admin-order-error').textContent='';openDialog('admin-order-dialog');
  }

  function showCategory(){
    requireAccess();if(saving)return;
    $('#admin-category-form').reset();$('#admin-category-error').textContent='';openDialog('admin-category-dialog');
  }

  function showProduct(id=null){
    requireAccess();if(saving)return;
    const product=id===null?null:state.products.find(item=>item.id===id);
    if(id!==null&&!product)return;
    if(!product&&!state.categories.length){showCategory();return;}
    editingId=id;
    const form=$('#admin-product-form');form.reset();
    const field=name=>form.elements.namedItem(name);
    field('categoryId').innerHTML=state.categories.map(category=>`<option value="${category.id}">${esc(category.name)}</option>`).join('');
    field('categoryId').disabled=Boolean(product);
    $('#admin-category-hint').textContent=product?'Категория назначена при создании товара.':'Нет нужной категории? Сначала создай её в разделе «Категории».';
    if(product){
      for(const name of ['name','description','price','stock','imageUrl'])field(name).value=product[name]??'';
      if(product.category)field('categoryId').value=product.category.id;
      else field('categoryId').innerHTML='<option>Без категории</option>';
    }
    $('#admin-product-title').textContent=product?'Редактировать товар':'Добавить товар';
    $('#admin-product-submit').textContent=product?'Сохранить изменения':'Добавить товар';
    $('#admin-product-error').textContent='';openDialog('admin-product-dialog');
  }

  function showDelete(id){
    requireAccess();if(saving)return;
    const product=state.products.find(item=>item.id===id);if(!product)return;
    deletingId=id;$('#admin-delete-error').textContent='';
    $('#admin-delete-description').textContent=`«${product.name}» исчезнет из каталога.`;
    openDialog('admin-delete-dialog');
  }

  function showRole(id){
    requireOwner();if(saving)return;
    const user=users.find(item=>item.userId===id);
    if(!user||user.role==='OWNER')return;
    const nextRole=user.role==='ADMIN'?'USER':'ADMIN';
    pendingRole={id:user.userId,role:nextRole};
    const promoting=nextRole==='ADMIN';
    $('#admin-role-title').textContent=promoting?'Назначить администратора?':'Снять роль администратора?';
    $('#admin-role-description').textContent=promoting
      ?`${user.name} получит доступ к товарам, категориям и списку пользователей.`
      :`${user.name} потеряет доступ к управлению магазином.`;
    $('#admin-role-hint').textContent='Изменение роли применяется при следующем входе пользователя. Текущий JWT действует до окончания своей сессии.';
    $('#admin-role-submit').textContent=promoting?'Назначить':'Снять роль';
    $('#admin-role-error').textContent='';openDialog('admin-role-dialog');
  }

  async function save({buttonId,errorId,dialogId,request,success,reload=loadCatalog}){
    if(saving)return;
    const button=$('#'+buttonId),error=$('#'+errorId),label=button.textContent;
    error.textContent='';saving=true;button.disabled=true;button.textContent='Сохраняем…';
    document.querySelectorAll(`#${dialogId} [data-close]`).forEach(element=>element.disabled=true);
    try{
      requireAccess();await request();$('#'+dialogId).close();toast(success);await reload();
    }catch(failure){
      error.textContent=failure.status===403
        ?'Недостаточно прав для этого действия.'
        :failure.status>=500
          ?'Не удалось подтвердить изменение. Обнови данные перед повторной попыткой.'
          :failure.message;
    }finally{
      saving=false;button.disabled=false;button.textContent=label;
      document.querySelectorAll(`#${dialogId} [data-close]`).forEach(element=>element.disabled=false);
    }
  }

  $('#admin-product-form').addEventListener('submit',event=>{
    event.preventDefault();
    const data=new FormData(event.currentTarget),id=editingId;
    const body={name:String(data.get('name')||'').trim(),description:String(data.get('description')||'').trim(),price:Number(data.get('price')),stock:Number(data.get('stock'))};
    body.imageUrl=String(data.get('imageUrl')||'').trim()||null;
    if(body.imageUrl){
      try{
        const url=new URL(body.imageUrl);
        if(url.protocol!=='https:'||!url.hostname||url.username||url.password||body.imageUrl.length>255)throw new Error();
      }catch{
        $('#admin-product-error').textContent='Укажи HTTPS-ссылку без логина и пароля, длиной до 255 символов.';
        return;
      }
    }
    let error='';
    if(!body.name)error='Укажи название товара.';
    else if(!Number.isFinite(body.price)||body.price<=0)error='Цена должна быть больше нуля.';
    else if(!Number.isInteger(body.stock)||body.stock<0||body.stock>2147483647)error='Остаток должен быть целым неотрицательным числом.';
    else if(state.products.some(product=>product.id!==id&&product.name.toLocaleLowerCase('ru')===body.name.toLocaleLowerCase('ru')))error='Товар с таким названием уже есть. Найди его в списке и нажми «Изменить».';
    if(id===null){body.categoryId=Number(data.get('categoryId'));if(!state.categories.some(category=>category.id===body.categoryId))error='Выбери категорию товара.';}
    if(error){$('#admin-product-error').textContent=error;return;}
    save({buttonId:'admin-product-submit',errorId:'admin-product-error',dialogId:'admin-product-dialog',request:()=>api(id===null?'/products':`/products/${id}`,{auth:true,method:id===null?'POST':'PUT',body:JSON.stringify(body)}),success:id===null?'Товар добавлен':'Изменения сохранены'});
  });

  $('#admin-category-form').addEventListener('submit',event=>{
    event.preventDefault();const data=new FormData(event.currentTarget);
    const body={name:String(data.get('name')||'').trim(),description:String(data.get('description')||'').trim()};
    if(!body.name){$('#admin-category-error').textContent='Укажи название категории.';return;}
    if(state.categories.some(category=>category.name.toLocaleLowerCase('ru')===body.name.toLocaleLowerCase('ru'))){$('#admin-category-error').textContent='Такая категория уже есть.';return;}
    save({buttonId:'admin-category-submit',errorId:'admin-category-error',dialogId:'admin-category-dialog',request:()=>api('/categories',{auth:true,method:'POST',body:JSON.stringify(body)}),success:'Категория создана. Теперь можно добавить товар.'});
  });

  $('#admin-delete-submit').addEventListener('click',()=>{
    const id=deletingId;if(id===null)return;
    save({buttonId:'admin-delete-submit',errorId:'admin-delete-error',dialogId:'admin-delete-dialog',request:()=>api(`/products/${id}`,{auth:true,method:'DELETE'}),success:'Товар удалён'});
  });

  $('#admin-role-submit').addEventListener('click',()=>{
    const change=pendingRole;if(!change)return;
    save({
      buttonId:'admin-role-submit',
      errorId:'admin-role-error',
      dialogId:'admin-role-dialog',
      request:()=>api(`/users/${change.id}/role`,{auth:true,method:'PATCH',body:JSON.stringify({role:change.role})}),
      success:change.role==='ADMIN'?'Администратор назначен. Пользователю нужно войти заново.':'Роль администратора снята.',
      reload:loadUsers,
    });
  });

  $('#admin-order-submit').addEventListener('click',()=>{
    const change=pendingOrder;if(!change)return;
    save({
      buttonId:'admin-order-submit',
      errorId:'admin-order-error',
      dialogId:'admin-order-dialog',
      request:()=>api(`/orders/${change.id}/status`,{auth:true,method:'PATCH',body:JSON.stringify({status:change.status})}),
      success:change.status==='PAID'?'Заказ отмечен оплаченным.':'Заказ отмечен отправленным.',
      reload:()=>loadOrders(ordersPage),
    });
  });

  async function refresh(){
    if(refreshing||saving)return;
    refreshing=true;$('#admin-refresh').disabled=true;
    try{
      if(tab==='users')await loadUsers();
      else if(tab==='orders')await loadOrders(ordersPage);
      else await loadCatalog();
    }finally{
      refreshing=false;$('#admin-refresh').disabled=false;
    }
  }

  $('#admin-view').addEventListener('click',event=>{
    const button=event.target.closest('button');if(!button||button.disabled)return;
    try{
      if(button.dataset.adminTab){
        tab=button.dataset.adminTab;$('#admin-search').value='';render();
        if(tab==='users')loadUsers();
        if(tab==='orders')loadOrders(0);
      }
      if(button.id==='admin-create'){if(tab==='products')showProduct();else if(tab==='categories')showCategory();}
      if(button.dataset.adminEdit)showProduct(Number(button.dataset.adminEdit));
      if(button.dataset.adminDelete)showDelete(Number(button.dataset.adminDelete));
      if(button.dataset.adminRole)showRole(Number(button.dataset.adminRole));
      if(button.dataset.adminOrderStatus)showOrderStatus(Number(button.dataset.adminOrderStatus));
      if(button.dataset.ordersPage)loadOrders(Number(button.dataset.ordersPage));
      if(button.id==='admin-refresh'||button.hasAttribute('data-admin-retry'))refresh();
      if(button.hasAttribute('data-users-retry'))loadUsers();
      if(button.hasAttribute('data-orders-retry'))loadOrders(ordersPage);
    }catch(error){toast(error.message);}
  });

  $('#admin-search').addEventListener('input',render);
  for(const id of ['admin-product-dialog','admin-category-dialog','admin-delete-dialog','admin-role-dialog','admin-order-dialog']){
    const dialog=$('#'+id);
    dialog.addEventListener('cancel',event=>{if(saving)event.preventDefault();});
    dialog.addEventListener('click',event=>{if(saving&&event.target===dialog)event.stopImmediatePropagation();},true);
  }
  return {render};
}
